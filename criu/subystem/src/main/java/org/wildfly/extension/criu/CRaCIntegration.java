/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.wildfly.extension.criu.Messages.ROOT_LOGGER;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Set;

import org.crac.CheckpointException;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.crac.RestoreException;
import org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.ElapsedTime;

/**
 * Integrates the WildFly kernel as an {@link org.crac.Resource}.
 */
final class CRaCIntegration {

    private static final String CRIU_TYPE = "OpenJDK CRaC";
    private static boolean hasCrac() {
        // Ideally org.crac.Core would expose an API to see
        // 1) if there's a backing impl and
        // 2) it's enabled
        // but absent that we decide ourselves based on what we know about how OpenJDK CRaC works.
        if (hasCoreClasses()) {
            if (isCracEnabled()) {
                return true;
            } else {
                ROOT_LOGGER.criuImplementationDisabled(CRIU_TYPE, "");
            }
        }
        return false;
    }

    private static boolean hasCoreClasses() {
        String cracCore = "org.crac.Core";
        try {
            CRaCIntegration.class.getClassLoader().loadClass(cracCore);
            // We loaded the facade class from org.crac (which may not have been provisioned);
            // now see if there's any backing class in the JVM.
            // NOTE: this may need updating if the expected backing classes change.
            try {
                cracCore = "javax.crac.Core";
                CRaCIntegration.class.getClassLoader().loadClass(cracCore);
                return true;
            }  catch (ClassNotFoundException e) {
                ROOT_LOGGER.debug(cracCore + " is not available" + e);
            }
            try {
                cracCore = "jdk.crac.Core";
                CRaCIntegration.class.getClassLoader().loadClass(cracCore);
                return true;
            }  catch (ClassNotFoundException e) {
                ROOT_LOGGER.debug(cracCore + " is not available" + e);
            }
        } catch (ClassNotFoundException e) {
            ROOT_LOGGER.debug(cracCore + " is not available" + e);
        }
        return false;
    }

    private static boolean isCracEnabled() {
        RuntimeMXBean mbean = ManagementFactory.getRuntimeMXBean();
        for (String arg : mbean.getInputArguments()) {
            if (arg.startsWith("-XX:CRaCCheckpointTo=")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Installs the OpenJDK CRaC integration.
     * @param supportedStrategies checkpointing strategies supported by the server kernel. Cannot be {@code null}.
     * @param processStateNotifier notifier for tracking process state changes. Cannot be {@code null}.
     * @param elapsedTime tracker for process elapsed time. Cannot be {@code null}.
     * @return a checkpoint integration object the management kernel can use, or {@code null} if the JDK CRaC
     *         integration classes are not available.
     */
    static CRIUExecutor install(Set<CheckpointStrategy> supportedStrategies,
                                ProcessStateNotifier processStateNotifier, ElapsedTime elapsedTime) {
        return hasCrac() ? ModelControllerResource.install(supportedStrategies, processStateNotifier, elapsedTime) : null;
    }

    private static class ModelControllerResource extends CheckpointIntegrationImpl implements Resource {

        private static CRIUExecutor install(Set<CheckpointStrategy> supportedStrategies,
                                            ProcessStateNotifier processStateNotifier, ElapsedTime elapsedTime) {
            ModelControllerResource mcr = new ModelControllerResource(supportedStrategies, processStateNotifier, elapsedTime);
            Core.getGlobalContext().register(mcr);
            ROOT_LOGGER.criuImplementationEnabled(CRIU_TYPE);
            return mcr;
        }

        private ModelControllerResource(final Set<CheckpointStrategy> supportedStrategies,
                                        final ProcessStateNotifier processStateNotifier, ElapsedTime elapsedTime) {
            super(supportedStrategies, processStateNotifier, elapsedTime);
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) {
            beforeCheckpoint();
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) {
            afterRestore();
        }

        @Override
        void executeCheckpoint() throws RestoreException, CheckpointException {
            Core.checkpointRestore();
        }
    }
}
