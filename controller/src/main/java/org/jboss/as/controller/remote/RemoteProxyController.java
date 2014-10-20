/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.controller.remote;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ERROR_CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationErrorCode;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;

/**
 * Remote {@link ProxyController} implementation.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public class RemoteProxyController implements ProxyController {

    private final PathAddress pathAddress;
    private final ProxyOperationAddressTranslator addressTranslator;
    private final TransactionalProtocolClient client;

    private RemoteProxyController(final TransactionalProtocolClient client, final PathAddress pathAddress,
                                  final ProxyOperationAddressTranslator addressTranslator) {
        this.client = client;
        this.pathAddress = pathAddress;
        this.addressTranslator = addressTranslator;
    }

    /**
     * Create a new remote proxy controller.
     *
     * @param client the transactional protocol client
     * @param pathAddress the path address
     * @param addressTranslator the address translator
     * @return the proxy controller
     */
    public static RemoteProxyController create(final TransactionalProtocolClient client, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator) {
        return new RemoteProxyController(client, pathAddress, addressTranslator);
    }

    /**
     * Creates a new remote proxy controller using an existing channel.
     *
     * @param channelAssociation the channel association
     * @param pathAddress the address within the model of the created proxy controller
     * @param addressTranslator the translator to use translating the address for the remote proxy
     * @return the proxy controller
     */
    public static RemoteProxyController create(final ManagementChannelHandler channelAssociation, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator) {
        final TransactionalProtocolClient client = TransactionalProtocolHandlers.createClient(channelAssociation);
        // the remote proxy
        return create(client, pathAddress, addressTranslator);
    }

    /**
     * Get the underlying transactional protocol client.
     *
     * @return the protocol client
     */
    public TransactionalProtocolClient getTransactionalProtocolClient() {
        return client;
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return pathAddress;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ModelNode original, final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments) {
        // Add blocking support to adhere to the proxy controller API contracts
        final CountDownLatch completed = new CountDownLatch(1);
        final BlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>> queue = new ArrayBlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>>(1, true);
        final TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation> operationListener = new TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation>() {
            @Override
            public void operationPrepared(TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation> prepared) {
                if(! queue.offer(prepared)) {
                    prepared.rollback();
                }
            }

            @Override
            public void operationFailed(TransactionalProtocolClient.Operation operation, ModelNode result) {
                try {
                    queue.offer(new BlockingQueueOperationListener.FailedOperation<TransactionalProtocolClient.Operation>(operation, result));
                } finally {
                    // This might not be needed?
                    completed.countDown();
                }
            }

            @Override
            public void operationComplete(TransactionalProtocolClient.Operation operation, ModelNode result) {
                try {
                    control.operationCompleted(result);
                } finally {
                    // Make sure the handler is called before commit/rollback returns
                    completed.countDown();
                }
            }
        };
        Future<ModelNode> futureResult = null;
        try {
            // Translate the operation
            final ModelNode translated = translateOperationForProxy(original);

            // Validate the address
//            if (validateAddresses && !validateAddress(translated, messageHandler)) {
//                throw ControllerMessages.MESSAGES.managementResourceNotFound(PathAddress.pathAddress(translated.get(OP_ADDR)));
//            }

            // Execute the operation
            ControllerLogger.MGMT_OP_LOGGER.tracef("Executing %s for %s", translated.get(OP).asString(), getProxyNodeAddress());
            futureResult = client.execute(operationListener, translated, messageHandler, attachments);
            // Wait for the prepared response
            final TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation> prepared = queue.take();
            if(prepared.isFailed()) {
                // If the operation failed, there is nothing more to do
                control.operationFailed(prepared.getPreparedResult());
                return;
            }
            // Send the prepared notification and wrap the OperationTransaction to block on commit/rollback
            final Future cancellable = futureResult;
            control.operationPrepared(new ModelController.OperationTransaction() {
                @Override
                public void commit() {
                    prepared.commit();
                    try {
                        // Await the completed notification
                        completed.await();
                    } catch(InterruptedException e) {
                        cancellable.cancel(true);
                        ControllerLogger.MGMT_OP_LOGGER.interruptedAwaitingFinalResponse(translated.get(OP).asString(), getProxyNodeAddress());
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                @Override
                public void rollback() {
                    prepared.rollback();
                    try {
                        // Await the completed notification
                        completed.await();
                    } catch(InterruptedException e) {
                        cancellable.cancel(true);
                        ControllerLogger.MGMT_OP_LOGGER.interruptedAwaitingFinalResponse(translated.get(OP).asString(), getProxyNodeAddress());
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }, prepared.getPreparedResult());

        } catch (InterruptedException e) {
            if (futureResult != null) { // it won't be null, as IE can only be thrown after it's assigned
                ControllerLogger.MGMT_OP_LOGGER.interruptedAwaitingInitialResponse(original.get(OP).asString(), getProxyNodeAddress());
                // Cancel the operation
                futureResult.cancel(true);
            }
            control.operationFailed(getCancelledResponse());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            final ModelNode result = new ModelNode();
            result.get(OUTCOME).set(FAILED);
            result.get(FAILURE_DESCRIPTION).set(e.getLocalizedMessage());
            // Notify the proxy control that the operation failed
            control.operationFailed(result);
        }
    }

    /**
     * Translate the operation address.
     *
     * @param op the operation
     * @return the new operation
     */
    public ModelNode translateOperationForProxy(final ModelNode op) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathAddress translated = addressTranslator.translateAddress(addr);
        if (addr.equals(translated)) {
            return op;
        }
        final ModelNode proxyOp = op.clone();
        proxyOp.get(OP_ADDR).set(translated.toModelNode());
        return proxyOp;
    }

    private static ModelNode getCancelledResponse() {
        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(CANCELLED);
        result.get(FAILURE_DESCRIPTION).set(ControllerLogger.ROOT_LOGGER.operationCancelled());
        result.get(ERROR_CODE).set(OperationErrorCode.StandardErrorCodes.CANCELLED.getCode());
        return result;
    }
}
