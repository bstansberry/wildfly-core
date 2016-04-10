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

import java.io.DataInput;
import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolConnectionManager;
import org.jboss.as.protocol.ProtocolConnectionUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.FutureManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.RemotingOptions;
import org.jgroups.Version;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Establishes a JBoss Remoting {@link Channel} to a remote Host Contoller's management
 * interface socket, and uses the management protocol to request that the remote HC
 * establish a {@link DomainConnection}.
 *
 * @author Brian Stansberry
 */
class JGroupsDomainConnector extends FutureManagementChannel {

    private static final Logger log = Logger.getLogger(JGroupsDomainConnector.class.getCanonicalName());

    private static final OptionMap options = OptionMap.builder()
            .set(RemotingOptions.HEARTBEAT_INTERVAL, 15000)
            .set(Options.READ_TIMEOUT, 45000)
            .getMap();

    private final ProtocolConnectionManager connectionManager;
    private final ManagementChannelHandler channelHandler;
    private final DomainConnection domainConnection;

    JGroupsDomainConnector(final DomainConnection domainConnection)  {
        this.domainConnection = domainConnection;
        DomainServer domainServer = domainConnection.getDomainServer();
        this.channelHandler = new ManagementChannelHandler(this, domainServer.getExecutorService(), domainConnection);
        this.connectionManager = ProtocolConnectionManager.create(new InitialConnectTask());
    }

    /**
     * Try to connect to the remote host.
     *
     * @throws IOException
     */
    void connect() throws IOException {
        connectionManager.connect();
    }

    @Override
    public void connectionOpened(Connection connection) throws IOException {
        final Channel channel = openChannel(connection, JGroupsProtocol.CHANNEL_NAME, options);
        if (setChannel(channel)) {
            channel.receiveMessage(channelHandler.getReceiver());
            channel.addCloseHandler(channelHandler);
            try {
                // Start the registration process
                channelHandler.executeRequest(new JGroupsConnectRequest(), null).getResult().get();
            } catch (Exception e) {
                if(e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException(e);
            }
            // Registered
            domainConnection.registered(channelHandler);
        } else {
            channel.closeAsync();
        }
    }

    /**
     * Connect and register at the remote domain controller.
     *
     * @return connection the established connection
     * @throws IOException
     */
    private Connection openConnection() throws IOException {
        // Perhaps this can just be done once?
        CallbackHandler callbackHandler = null;
        SSLContext sslContext = null;
        DomainServer domainServer = domainConnection.getDomainServer();
        SecurityRealm realm = domainServer.getSecurityRealm();
        if (realm != null) {
            sslContext = realm.getSSLContext();
            CallbackHandlerFactory handlerFactory = realm.getSecretCallbackHandlerFactory();
            if (handlerFactory != null) {
                String domainServerUsername = domainServer.getUserName();
                callbackHandler = handlerFactory.getCallbackHandler(domainServerUsername);
            }
        }

        final ProtocolConnectionConfiguration config = new ProtocolChannelClient.Configuration();
        config.setEndpoint(domainServer.getEndpoint());
        config.setOptionMap(options);
        config.setCallbackHandler(callbackHandler);
        config.setSslContext(sslContext);
        config.setUri(domainConnection.peerAddress().toURI());
        // Connect
        return ProtocolConnectionUtils.connectSync(config);
    }

    private class InitialConnectTask implements ProtocolConnectionManager.ConnectTask {

        @Override
        public Connection connect() throws IOException {
            return openConnection();
        }

        @Override
        public ProtocolConnectionManager.ConnectionOpenHandler getConnectionOpenedHandler() {
            return JGroupsDomainConnector.this;
        }

        @Override
        public ProtocolConnectionManager.ConnectTask connectionClosed() {
            // TODO perhaps debug log; or should we return null?
            return this;
        }

        @Override
        public void shutdown() {
            //
        }
    }

    /**
     * The request to establish a {@link org.jboss.as.host.controller.jgroups.DomainConnection} on
     * the remote side.
     */
    private class JGroupsConnectRequest extends AbstractManagementRequest<Void, Void> {

        @Override
        public byte getOperationType() {
            return JGroupsProtocol.JGROUPS_CONNECT;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            DomainAddress ourAddress = (DomainAddress) domainConnection.localAddress();
            try {
                output.writeShort(Version.version);
                ourAddress.writeTo(output);
                domainConnection.updateLastAccessed();
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
            byte param = input.readByte();
            // If it failed
            if (param != JGroupsProtocol.OK) {
                // TODO some error handling
//                final byte errorCode = input.readByte();
//                final String message =  input.readUTF();
//                resultHandler.failed(new SlaveRegistrationException(SlaveRegistrationException.ErrorCode.parseCode(errorCode), message));
                resultHandler.failed(new IllegalStateException());
                log.trace("JGROUP_CONNECT failed");
            } else {
                log.trace("JGROUP_CONNECT is OK");
                resultHandler.done(null);
            }
        }
    }
}
