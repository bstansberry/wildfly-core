/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.host.controller.jgroups;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.host.controller.HostControllerService;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

/**
 * Installs a {@link DomainServer}.
 *
 * @author Brian Stansberry
 */
public class DomainServerService implements Service<DomainServer> {

    public static final ServiceName SERVICE_NAME = HostControllerService.HC_SERVICE_NAME.append("group-communications-handler");

    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();
    private final InjectedValue<SecurityRealm> securityRealmInjector = new InjectedValue<SecurityRealm>();
    private final InjectedValue<NetworkInterfaceBinding> interfaceInjector = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<ExecutorService>();

    private final LocalHostControllerInfo localHostInfo;
    private final String protocol;
    private DomainServer domainServer;

    public static void install(ServiceTarget target,
                               LocalHostControllerInfo localHostInfo,
                               String protocol) {
        String niName;
        String realmName;
        switch (protocol) {
            case "remote": {
                niName =   localHostInfo.getNativeManagementInterface();
                realmName = localHostInfo.getNativeManagementSecurityRealm();
                break;
            }
            case "http-remoting": {
                niName =   localHostInfo.getHttpManagementInterface();
                realmName = localHostInfo.getHttpManagementSecurityRealm();
                break;
            }
            case "https-remoting": {
                niName =   localHostInfo.getHttpManagementSecureInterface();
                realmName = localHostInfo.getHttpManagementSecurityRealm();
                break;
            }
            default:
                throw new IllegalStateException();
        }

        DomainServerService service = new DomainServerService(localHostInfo, protocol);
        ServiceBuilder<DomainServer> builder = target.addService(SERVICE_NAME, service)
                .addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpointInjector)
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(niName), NetworkInterfaceBinding.class, service.interfaceInjector)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, service.executorServiceInjector);
        if (realmName != null) {
            builder = builder.addDependency(SecurityRealm.ServiceUtil.createServiceName(realmName), SecurityRealm.class, service.securityRealmInjector);
        }
        builder.install();

        ManagementRemotingServices.installManagementChannelOpenListenerService(target, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                JGroupsProtocol.CHANNEL_NAME, SERVICE_NAME, OptionMap.EMPTY, false);
    }

    private DomainServerService(LocalHostControllerInfo localHostInfo, String protocol) {
        this.localHostInfo = localHostInfo;
        this.protocol = protocol;
    }


    @Override
    public synchronized void start(StartContext context) throws StartException {
        DomainAddress localAddress = getLocalAddress();
        String realmName = localHostInfo.getRemoteDomainControllerUsername();
        String name = realmName != null ? realmName : localHostInfo.getLocalHostName();
        domainServer = new DomainServer(localAddress, endpointInjector.getValue(),
                securityRealmInjector.getOptionalValue(),
                name, executorServiceInjector.getValue());
    }

    @Override
    public synchronized void stop(StopContext context) {
        domainServer = null;
    }

    @Override
    public DomainServer getValue() throws IllegalStateException, IllegalArgumentException {
        return domainServer;
    }

    private DomainAddress getLocalAddress() {
        InetAddress address = interfaceInjector.getValue().getAddress();
        int port;
        switch (protocol) {
            case "remote": {
                port = localHostInfo.getNativeManagementPort();
                break;
            }
            case "http-remoting": {
                port = localHostInfo.getHttpManagementPort();
                break;
            }
            case "https-remoting": {
                port = localHostInfo.getHttpManagementSecurePort();
                break;
            }
            default:
                throw new IllegalStateException();
        }
        return new DomainAddress(protocol, address, port);
    }
}
