/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.server;

import java.util.Set;

import org.jboss.as.controller.ModelController.CheckpointIntegration;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.version.Stability;

/**
 * Factory for creating a {@link CheckpointIntegration}. The expectation is implementations,
 * if available, will be loaded via a {@code ServiceLoader}.
 */
public interface CheckpointIntegrationFactory {

    /**
     * Create a {@link org.jboss.as.controller.ModelController.CheckpointIntegration}.
     *
     * @param stability the stability level of this process. Cannot be {@code null}.
     * @param supportedStrategies checkpointing strategies supported by this server kernel. Cannot be {@code null}.
     * @param processStateNotifier notifier for tracking process state changes. Cannot be {@code null}.
     * @param suspendController suspend controller to use for suspend/resume based checkpointing. Cannot be {@code null}.
     * @param elapsedTime the {@code ServerEnvironment} elapsed time object
     *
     * @return a checkpoint integration object the management kernel can use. Will not be {@code null}.
     */
    CheckpointIntegration create(Stability stability, Set<CheckpointIntegration.CheckpointStrategy> supportedStrategies,
                                 ProcessStateNotifier processStateNotifier, SuspendController suspendController,
                                 ElapsedTime elapsedTime);

}
