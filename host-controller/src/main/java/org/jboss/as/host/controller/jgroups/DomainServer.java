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

import java.util.concurrent.ExecutorService;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jgroups.Address;
import org.jgroups.blocks.cs.BaseServer;
import org.jgroups.blocks.cs.Connection;

/**
 * JGroups {@link BaseServer} subclass that manages connections using the
 * JBoss Remoting domain management channels.
 *
 * @author Brian Stansberry
 */
final class DomainServer extends BaseServer implements ManagementChannelInitialization {

    private static final Logger log = Logger.getLogger(DomainServer.class.getCanonicalName());

    private final Endpoint endpoint;
    private final SecurityRealm realm;
    private final String userName;
    private final ExecutorService executorService;

    DomainServer(DomainAddress localAddress, Endpoint endpoint, SecurityRealm realm, String userName,
                 ExecutorService executorService) {
        super(null);
        this.endpoint = endpoint;
        this.realm = realm;
        this.userName = userName;
        this.executorService = executorService;
        this.local_addr = localAddress;
    }

    @Override
    public void start() throws Exception {
        if(running.compareAndSet(false, true)) {
            super.start();
        }
    }

    @Override
    public void stop() {
        if(running.compareAndSet(true, false)) {
            super.stop();
        }
    }

    @Override
    protected Connection createConnection(Address dest) throws Exception {
        log.tracef("Creating connection to %s", dest);
        return new DomainConnection((DomainAddress) dest, this);
    }

    @Override
    public ManagementChannelShutdownHandle startReceiving(Channel channel) {

        final ManagementChannelHandler handler = new ManagementChannelHandler(ManagementClientChannelStrategy.create(channel),
                executorService);
        if (running()) {
            log.tracef("Channel for %s is starting receiving", JGroupsProtocol.CHANNEL_NAME);
            DomainConnection connection = new DomainConnection(handler, this);
            handler.addHandlerFactory(connection);
            channel.receiveMessage(handler.getReceiver());
        } else {
            log.tracef("Not running; closing channel for %s", JGroupsProtocol.CHANNEL_NAME);
            StreamUtils.safeClose(channel);
        }
        return handler;
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    SecurityRealm getSecurityRealm() {
        return realm;
    }

    /** Gets the username to use when authenticating intradomain connections */
    String getUserName() {
        return userName;
    }

    Endpoint getEndpoint() {
        return endpoint;
    }
}
