/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller;

import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.security.ControllerPermission;
import org.jboss.dmr.ModelNode;

/**
 * Controls reads of and modifications to a management model.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModelController {

    /**
     * A {@link org.jboss.as.controller.security.ControllerPermission} needed to access a {@link ModelController} via {@link org.jboss.msc.service.Service#getValue()} or
     * to invoke its methods. The name of the permission is "{@code canAccessModelController}."
     */
    Permission ACCESS_PERMISSION = ControllerPermission.CAN_ACCESS_MODEL_CONTROLLER;

    /**
     * Execute an operation, sending updates to the given handler. This method is not intended to be invoked directly
     * by clients.
     *
     * @param operation the operation to execute
     * @param handler the message handler
     * @param control the transaction control for this operation
     * @param attachments the operation attachments
     * @return the operation result
     *
     * @throws SecurityException if the caller does not have {@link #ACCESS_PERMISSION}
     */
    ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments);

    /**
     * Execute an operation, sending updates to the given handler, and making available in the return value
     * any streams that may have been associated with the response.  This method is not intended to be invoked directly
     * by clients.
     *
     * @param operation the operation to execute
     * @param handler the message handler
     * @param control the transaction control for this operation
     * @return the operation response
     *
     * @throws SecurityException if the caller does not have {@link #ACCESS_PERMISSION}
     */
    OperationResponse execute(Operation operation, OperationMessageHandler handler, OperationTransactionControl control);

    /**
     * A callback interface for the operation's completion status.  Implemented in order to control whether a complete
     * operation is committed or rolled back after it is prepared.
     */
    interface OperationTransactionControl {

        /**
         * Notify that an operation is complete and may be committed or rolled back.
         *
         * <p><strong>It is the responsibility of the user of this {@code OperationTransactionControl} to ensure that
         * {@link OperationTransaction#commit()} or {@link OperationTransaction#rollback()} is eventually called on
         * the provided {@code transaction}.
         * </strong></p>
         *
         * @param transaction the transaction to control the fate of the operation. Cannot be {@code null}
         * @param result the result. Cannot be {@code null}
         * @param context the {@code OperationContext} for the operation that is prepared
         */
        default void operationPrepared(OperationTransaction transaction, ModelNode result, OperationContext context) {
            operationPrepared(transaction, result);
        }

        /**
         * Notify that an operation is complete and may be committed or rolled back.
         *
         * <p><strong>It is the responsibility of the user of this {@code OperationTransactionControl} to ensure that
         * {@link OperationTransaction#commit()} or {@link OperationTransaction#rollback()} is eventually called on
         * the provided {@code transaction}.
         * </strong></p>
         *
         * @param transaction the transaction to control the fate of the operation. Cannot be {@code null}
         * @param result the result. Cannot be {@code null}
         */
        void operationPrepared(OperationTransaction transaction, ModelNode result);

        /**
         * An operation transaction control implementation which always commits the operation.
         */
        OperationTransactionControl COMMIT = new OperationTransactionControl() {
            public void operationPrepared(final OperationTransaction transaction, final ModelNode result) {
                transaction.commit();
            }
        };
    }

    /**
     * An operation transaction.
     */
    interface OperationTransaction {

        /**
         * Commit the operation.
         */
        void commit();

        /**
         * Roll the operation back.
         */
        void rollback();
    }

    /**
     * Integration between a {@code ModelController} and JVM checkpointing logic.
     */
    interface CheckpointIntegration {

        /** Implementation instance whose methods are no-ops. */
        CheckpointIntegration NOOP_INTEGRATION = new CheckpointIntegration() {};

        /**
         * Provide the checkpoint integration with the facilities it needs to execute management operations.
         *
         * @param clientFactory       factory to create a {@code ModelControllerClient}. Cannot be {@code null}.
         * @param clientExecutor      executor the {@code ModelControllerClient} should use. Cannot be {@code null}.
         */
        default void register(ModelControllerClientFactory clientFactory, Executor clientExecutor) {
            // no-op
        }

        /**
         * Gets the currently effective checkpoint strategy.
         *
         * @return the strategy, or {@code null} if no checkpoint preparation is taking place.
         */
        default CheckpointStrategy getCurrentCheckpointStrategy() {
            return null;
        }

        /**
         * Indicates a server boot has reached a stage where the caller is ready for a JVM checkpoint.
         * This should be called by a thread that if blocked will prevent further progress on the
         * server boot.
         */
        default void readyForCheckpoint() {
            // no-op
        }

        /**
         * Strategies to use when preparing the process for a JVM checkpoint.
         */
        enum CheckpointStrategy {
            /**
             * Prepare for a checkpoint by initiating a server reload and stopping the reload and
             * allowing the checkpoint to proceed when the boot of the reloaded server has reached
             * the point that the server configuration model has been established. Following a
             * JVM restore, the boot operation is allowed to continue.
             */
            RELOAD_TO_MODEL("reload-to-model"),
            /**
             * Prepare for a checkpoint by initiating a server reload and stopping the reload and
             * allowing the checkpoint to proceed when the boot of the reloaded server has reached
             * the point that initial deployment processing has completed, but most container
             * and deployment runtime initialization has not yet begun. Following a
             * JVM restore, the boot operation is allowed to continue.
             */
            RELOAD_TO_DEPLOYMENT_INIT("reload-to-deployment-init"),
            /**
             * Prepare for a JVM checkpoint by suspending the server, waiting as long as needed
             * for the server to suspend. Following a JVM restore, the server is resumed.
             */
            SUSPEND_RESUME("suspend-resume")
            ;

            private final String name;

            CheckpointStrategy(String name) {
                this.name = name;
            }

            public String toString() {
                return name;
            }

            private static final Map<String, CheckpointStrategy> MAP = Collections.synchronizedMap(new HashMap<>());

            public static CheckpointStrategy fromString(String name) {
                synchronized (MAP) {
                    if (MAP.isEmpty()) {
                        for (CheckpointStrategy strategy : values()) {
                            MAP.put(strategy.name, strategy);
                        }
                    }
                    CheckpointStrategy result = MAP.get(name);
                    if (result == null) {
                        throw new IllegalArgumentException(name);
                    }
                    return result;
                }
            }
        }
    }
}
