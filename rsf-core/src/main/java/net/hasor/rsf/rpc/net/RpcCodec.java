/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.rpc.net;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.more.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import net.hasor.rsf.address.InterAddress;
import net.hasor.rsf.domain.ProtocolStatus;
import net.hasor.rsf.domain.RsfException;
import net.hasor.rsf.transform.protocol.RequestInfo;
import net.hasor.rsf.transform.protocol.ResponseInfo;
import net.hasor.rsf.utils.TimerManager;
/**
 * 负责处理两个RSF程序之间的握手。
 * @version : 2014年11月4日
 * @author 赵永春(zyc@hasor.net)
 */
public class RpcCodec extends ChannelInboundHandlerAdapter {
    protected Logger               logger     = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean    shakeHands = new AtomicBoolean(false);
    private InterAddress           bindAddress;
    private InterAddress           targetAddress;
    private final TimerManager     rsfTimerManager;
    private final ChannelRegister  channelRegister;
    private final ReceivedListener rpcEventListener;
    //
    public RpcCodec(RsfNetManager rsfNetManager) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.bindAddress = rsfNetManager.bindAddress();
        this.rsfTimerManager = rsfNetManager.getTimerManager();
        this.channelRegister = rsfNetManager.getChannelRegister();
        this.rpcEventListener = rsfNetManager.getReceivedListener();
    }
    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        this.rsfTimerManager.atTime(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                if (shakeHands.get() == false) {
                    ctx.close();
                }
            }
        });
        super.handlerAdded(ctx);
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (this.shakeHands.get() == false) {
            RequestInfo request = new RequestInfo();
            request.setRequestID(-1);
            request.setTargetMethod("ASK_HOST_INFO");
            ctx.pipeline().writeAndFlush(request);//发送握手数据包
            super.channelActive(ctx);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.shakeHands.get() == true) {
            /*   */if (msg instanceof RequestInfo) {
                this.rpcEventListener.receivedMessage(this.targetAddress, (RequestInfo) msg);
            } else if (msg instanceof ResponseInfo) {
                this.rpcEventListener.receivedMessage(this.targetAddress, (ResponseInfo) msg);
            }
            return;
        }
        //
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.pipeline().channel().remoteAddress();
        if (msg instanceof RequestInfo) {
            RequestInfo request = (RequestInfo) msg;
            String cmdType = request.getTargetMethod();
            if (request.getRequestID() == -1 && StringUtils.equals(cmdType, "ASK_HOST_INFO")) {
                ResponseInfo response = new ResponseInfo();
                response.setRequestID(-1);
                response.setStatus(ProtocolStatus.OK);
                response.addOption("SERVER_INFO", this.bindAddress.toHostSchema());//RSF实例信息。
                logger.info("send ack to {}.", remoteAddress);
                ctx.pipeline().writeAndFlush(response);//发送握手数据包
                return;
            }
        }
        if (msg instanceof ResponseInfo) {
            ResponseInfo response = (ResponseInfo) msg;
            String serverInfo = response.getOption("SERVER_INFO");
            Channel channel = ctx.pipeline().channel();
            this.targetAddress = new InterAddress(serverInfo);
            this.shakeHands.set(true);
            RsfNetChannel netChannel = new RsfNetChannel(this.targetAddress, channel, this.shakeHands);
            this.channelRegister.completed(this.targetAddress, netChannel);
            logger.info("socket ready for {}.", this.targetAddress);
        }
    }
    //
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("close socket for {}.", this.targetAddress);
        this.channelRegister.failed(this.targetAddress, new RsfException(ProtocolStatus.NetworkError, "channel Inactive."));
        ctx.close();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("close socket=" + this.targetAddress + " with error -> " + cause.getMessage(), cause);
        this.channelRegister.failed(this.targetAddress, cause);
        ctx.close();
    }
}