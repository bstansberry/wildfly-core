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

import static org.jboss.as.host.controller.jgroups.JGroupsProtocol.JGROUPS_CONNECT;
import static org.jboss.as.host.controller.jgroups.JGroupsProtocol.JGROUPS_MESSAGE;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jboss.as.host.controller.logging.HostControllerLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jgroups.Address;
import org.jgroups.Version;
import org.jgroups.blocks.cs.Connection;
import org.xnio.IoUtils;

/**
 * JGroups {@link Connection} implementation that is based on sending
 * messages over a JBoss Remoting {@link Channel} connected via
 * the management interfaces.
 *
 * @author Brian Stansberry
 */
class DomainConnection extends Connection implements ManagementRequestHandlerFactory {

    private static final Logger log = Logger.getLogger(DomainConnection.class.getCanonicalName());

    private DomainAddress peer_addr;
    private volatile ManagementChannelHandler channelHandler;
    private volatile boolean open;
    private final DomainServer server;
    private final ManagementRequestHandler<Void, Void> connectHandler = new JGroupsConnectHandler();
    private final ManagementRequestHandler<Void, Void> messageHandler = new JGroupsMessageHandler();

    /**
     * Constructor for a connection that will be originated from this side.
     * @param address       the address of the remote HC
     * @param domainServer  the DomainServer managing the connections
     */
    DomainConnection(DomainAddress address, DomainServer domainServer) {
        this.peer_addr = address;
        this.server = domainServer;
        last_access=getTimestamp(); // last time a message was sent or received (ns)
    }

    /**
     * Constructor for a connection that was originated on the remote side.
     * @param channelHandler handler for sending messages to the remote peer
     * @param domainServer   the DomainServer managing the connections
     */
    DomainConnection(ManagementChannelHandler channelHandler, DomainServer domainServer) {
        this.channelHandler = channelHandler;
        this.open = true;
        this.server = domainServer;
        registerCloseHandler(channelHandler);
        last_access=getTimestamp(); // last time a message was sent or received (ns)
    }

    private void registerCloseHandler(ManagementChannelHandler channelHandler) {
        final Channel channel = getChannel(channelHandler);
        if (channel == null) throw new IllegalStateException();
        channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                if (channel == closed) {
                    open = false;
                    if (peer_addr != null) {
                        server.notifyConnectionClosed(DomainConnection.this, "Channel closed");
                    }
                }
            }
        });
    }

    @Override
    public boolean isOpen() {
        // Based on the TcpConnection impl, this means "whether there currently is an open connection"
        return open;
    }

    @Override
    public boolean isConnected() {
        // Based on the TcpConnection impl, this means "whether there ever was an open connection"
        return peer_addr != null && channelHandler != null;
    }

    @Override
    public Address localAddress() {
        return server.localAddress();
    }

    @Override
    public DomainAddress peerAddress() {
        return peer_addr;
    }

    @Override
    public boolean isExpired(long now) {
        long expTime = server.connExpireTime();
        return expTime > 0 && now - last_access >= expTime;
    }

    @Override
    public void connect(Address dest) throws Exception {

        log.tracef("Connecting to %s", peer_addr);

        JGroupsDomainConnector connector = new JGroupsDomainConnector(this);
        connector.connect();
//        synchronized (this) {
//            while (channelHandler == null) {
//                wait();
//            }
//        }

        log.infof("Connected to %s", peer_addr);
    }

    @Override
    public void start() throws Exception {
        // no-op
    }

    @Override
    public void send(byte[] buf, int offset, int length) throws Exception {
        JGroupsMessageRequest req = new JGroupsMessageRequest(buf, offset, length);
        channelHandler.executeRequest(req, null);
    }

    @Override
    public void send(ByteBuffer buf) throws Exception {

        int offset = buf.hasArray()? buf.arrayOffset() + buf.position() : buf.position();
        int len = buf.remaining();
        if (!buf.isDirect()) {
            send(buf.array(), offset, len);
        } else {
            byte[] tmp=new byte[len];
            buf.get(tmp, 0, len);
            send(tmp, 0, len);
        }
    }

    @Override
    public void close() throws IOException {
        open = false;
        IoUtils.safeClose(getChannel(channelHandler));
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header) {

        if (server.running()) {
            final byte operationId = header.getOperationId();
            switch (operationId) {
                case JGROUPS_CONNECT: {
                    handlers.registerActiveOperation(header.getBatchId(), null, null);
                    return connectHandler;
                }
                case JGROUPS_MESSAGE: {
                    handlers.registerActiveOperation(header.getBatchId(), null, null);
                    return messageHandler;
                }
            }
        }
        return handlers.resolveNext();
    }

    private Channel getChannel(ManagementChannelHandler channelHandler) {
        try {
            return channelHandler != null ? channelHandler.getChannel() : null;
        } catch (IOException e) {
            // Should not happen as we have a channel
            throw new IllegalStateException(e);
        }
    }

    DomainServer getDomainServer() {
        return server;
    }

    /**
     * Callback invoked by the {@link JGroupsDomainConnector} when the connection to the peer is established.
     * @param channelHandler the handler for sending messages to the peer
     */
    void registered(ManagementChannelHandler channelHandler) {
        registerCloseHandler(channelHandler);
        this.channelHandler = channelHandler;
        open = true;
        log.infof("Connection to %s registered", peer_addr);
//        // wake up the thread in connect
//        synchronized (this) {
//            notifyAll();
//        }
    }

    private long getTimestamp() {
        return server.timeService() != null? server.timeService().timestamp() : System.nanoTime();
    }

    void updateLastAccessed() {
        if (server.connExpireTime() > 0) {
            last_access = getTimestamp();
        }
    }

    private class JGroupsConnectHandler implements ManagementRequestHandler<Void, Void> {

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            log.trace("Received JGROUPS_CONNECT");

            // TcpConnection checks for the 'bela' cookie, but we don't bother with that
            // as we aren't directly reading from a socket. By the time this call gets here
            // the data on the socket has to have already included a bunch of correct stuff

            // Verify the version
            short version=input.readShort();
            if(!Version.isBinaryCompatible(version)) {
                throw HostControllerLogger.ROOT_LOGGER.incompatibleJGroupsVersion(context.getChannel().getConnection(),
                        Version.print(version), Version.printVersion());
            }
            peer_addr = readPeerAddress(input);
            server.replaceConnection(peer_addr, DomainConnection.this);

            final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
            final FlushableDataOutput output = context.writeMessage(header);
            try {
                output.write(JGroupsProtocol.OK);
                resultHandler.done(null);
                updateLastAccessed();
                log.tracef("Connected %s", peer_addr);
            } finally {
                StreamUtils.safeClose(output);
            }
        }

        private DomainAddress readPeerAddress(DataInput input) throws IOException {
            DomainAddress result = new DomainAddress();
            try {
                result.readFrom(input);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    private class JGroupsMessageHandler implements ManagementRequestHandler<Void, Void> {

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            log.tracef("Received JGROUPS_MESSAGE from %s", peer_addr);
            byte[] data = getMessageBytes(input);
            updateLastAccessed();
            server.receive(peer_addr, data, 0, data.length);
            resultHandler.done(null);
        }

        private byte[] getMessageBytes(DataInput input) throws IOException {
            int len = input.readInt();
            byte[] result = new byte[len];
            input.readFully(result);
            return result;
        }
    }

    /**
     * The request to send a message to the remote side.
     */
    private class JGroupsMessageRequest extends AbstractManagementRequest<Void, Void> {

        private final byte[] data;
        private final int offset;
        private final int length;

        private JGroupsMessageRequest(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public byte getOperationType() {
            return JGroupsProtocol.JGROUPS_MESSAGE;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            log.tracef("Sending JGROUPS_MESSAGE to %s", peer_addr);
            output.writeInt(length);
            output.write(data, offset, length);
            resultHandler.done(null);
            updateLastAccessed();
        }

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
            // no-op
        }
    }
}
