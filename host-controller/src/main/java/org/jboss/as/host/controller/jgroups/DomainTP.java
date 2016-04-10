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

import java.util.Collection;

import org.jgroups.Address;
import org.jgroups.PhysicalAddress;
import org.jgroups.annotations.ManagedOperation;
import org.jgroups.protocols.BasicTCP;

/**
 * JGroups {@link org.jgroups.protocols.TP} implementation that uses JBoss Remoting based
 * communication over the intra-domain communication links.
 * <p>
 * This class is based on {@link BasicTCP} as its behavior is consistent with a protocol
 * like JGroups' {@link org.jgroups.protocols.TCP}, i.e. it maintains a mesh of TCP-based connections
 * with all members. It differs in that it doesn't open server sockets (those are controlled
 * by the HC's management interface services) and JBoss Remoting and the management protocol
 * code lie between the JGroups code and the actual socket.
 *
 * @author Brian Stansberry
 */
class DomainTP extends BasicTCP {

    private final DomainServer server;

    DomainTP(DomainServer domainServer) {
        this.server = domainServer;
        this.id = 9999;
        // Hack to stop BasicTP init() complaining about bind_port = 0
        DomainAddress localAddress = (DomainAddress)domainServer.localAddress();
        this.bind_addr = localAddress.getHost();
        this.bind_port = localAddress.getPort();
    }

    @Override
    @ManagedOperation
    public String printConnections() {
        return server.printConnections();
    }

    @Override
    public void send(Address dest, byte[] data, int offset, int length) throws Exception {
        server.send(dest, data, offset, length);
    }

    @Override
    public void retainAll(Collection<Address> members) {
        server.retainAll(members);
    }

    @Override
    public void start() throws Exception {
        assert server != null;
        server.receiver(this)
                .timeService(time_service)
                .log(this.log);

        super.start();
    }

    @Override
    protected PhysicalAddress getPhysicalAddress() {
        return server != null ? (PhysicalAddress) server.localAddress() : null;
    }

    @Override
    protected void handleConnect() throws Exception {
        if(isSingleton()) {
            if(connect_count == 0) {
                server.start();
            }
            super.handleConnect();
        }
        else
            server.start();
    }

    @Override
    protected void handleDisconnect() {
        if(isSingleton()) {
            super.handleDisconnect();
            if(connect_count == 0)
                server.stop();
        }
        else
            server.stop();
    }
}
