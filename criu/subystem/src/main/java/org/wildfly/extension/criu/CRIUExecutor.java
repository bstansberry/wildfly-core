/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.wildfly.extension.criu.Messages.ROOT_LOGGER;

import org.jboss.as.controller.ModelController.CheckpointIntegration;
import org.jboss.as.controller.OperationFailedException;

/**
 * A {@link CheckpointIntegration} that also allows callers to configure the integration and trigger a JVM checkpoint.
 * The additional methods in this interface allow integration of the {@code CheckpointIntegration}
 * implementation with the WildFly management API.
 */
interface CRIUExecutor extends CheckpointIntegration {

    CRIUExecutor NOOP_EXECUTOR = new CRIUExecutor() {};


    /**
     * Returns whether the implementation supports {@link #triggerCheckpoint(CheckpointStrategy) triggering a checkpoint}.
     * @return {@code true} if {@link #triggerCheckpoint(CheckpointStrategy) triggering a checkpoint} can succeed;
     *         {@code false} if it will always result in an {@link OperationFailedException}
     */
    default boolean isCheckpointingSupported() {
        return false;
    }

    default void setDefaultCheckpointStrategy(CheckpointStrategy strategy) {
        // no-op
    }

    /**
     * Tell the JVM to create a checkpoint. This operation will block until the implementation
     * receives a notification that checkpoint preparation should begin. The operation will
     * then return, allowing the checkpoint to proceed.
     * </p>
     * @throws OperationFailedException if {@link #isCheckpointingSupported()} returns {@code false}
     * @throws RuntimeException if there is a problem triggering the checkpoint
     */
    default void triggerCheckpoint(CheckpointStrategy checkpointStrategy) throws OperationFailedException {
        throw ROOT_LOGGER.checkpointNotSupported();
    }

}
