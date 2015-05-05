/*
 * Copyright 2008-2009 the original 赵永春(zyc@hasor.net).
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
package net.hasor.rsf.center.server;
import net.hasor.rsf.center.client.HostType;
import net.hasor.rsf.center.client.ProxyInterface;
import net.hasor.rsf.center.client.ServiceMetaInfo;
/**
 * 
 * @version : 2015年4月28日
 * @author 赵永春(zyc@hasor.net)
 */
public class ProxyInterfaceImpl implements ProxyInterface {
    @Override
    public boolean onLine(ServiceMetaInfo serviceInfo, HostType hostType) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean offLine(ServiceMetaInfo serviceInfo, HostType hostType) {
        // TODO Auto-generated method stub
        return false;
    }
}