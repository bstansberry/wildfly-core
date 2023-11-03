/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.wildfly.common.Assert.checkNotNullParam;
import static org.wildfly.extension.criu.CRIUExecutor.NOOP_EXECUTOR;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import org.jboss.as.controller.ModelController.CheckpointIntegration;
import org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.CheckpointIntegrationFactory;
import org.jboss.as.server.ElapsedTime;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.version.Stability;

/**
 * Integrates the WildFly kernel with JVM checkpointing support, if any is available.
 */
public final class CRIUIntegration implements CheckpointIntegrationFactory {

    private static volatile CRIUExecutor installedExecutor = NOOP_EXECUTOR;

    static final Stability FEATURE_STABILITY = Stability.EXPERIMENTAL;

    private static volatile Path CRIU_DIR;
    private static volatile Path DUMP_DIR;
    private static volatile Path MARKER_FILE;

    @Override
    public CheckpointIntegration create(Stability stability, Set<CheckpointStrategy> supportedStrategies,
                                        ProcessStateNotifier processStateNotifier, SuspendController suspendController,
                                        ElapsedTime elapsedTime) {
        checkNotNullParam("stability", stability);
        checkNotNullParam("supportedStrategies", supportedStrategies);
        checkNotNullParam("processStateNotifier", processStateNotifier);
        checkNotNullParam("suspendController", suspendController); // currently unused
        checkNotNullParam("elapsedTime", elapsedTime);

        CRIUExecutor result;
        if (stability.enables(FEATURE_STABILITY)) {
            // OpenJ9 CRIU is a supported feature with an explicit API to see if it is enabled,
            // so prefer that if it is available and enabled.
            // TODO -- configurability
            // We could create a delegating object that provides both, and then use a subsystem
            // attribute to select which is wanted, defaulting to preferring J9 if both are available.
            // Subsystem add fails if the configured choice is not available. To support reusable config,
            // perhaps a second attribute as well to control whether having the impl enabled is required.
            result = J9Integration.install(supportedStrategies, processStateNotifier, elapsedTime);
            result = result != null ? result : CRaCIntegration.install(supportedStrategies, processStateNotifier, elapsedTime);
            result = result != null ? result : NOOP_EXECUTOR;
        } else {
            result = NOOP_EXECUTOR;
        }
        installedExecutor = result;
        return result;
    }

    static CRIUExecutor getInstalledExecutor() {
        return installedExecutor;
    }

    static Path getCriuPath() {
        if (CRIU_DIR == null) {
            Path dataDir = new File(System.getProperty("jboss.server.data.dir")).toPath();
            CRIU_DIR = dataDir.resolve("criu");
        }
        return CRIU_DIR;
    }
    static Path getDumpPath() {
        if (DUMP_DIR == null) {
            DUMP_DIR = getCriuPath().resolve("dump");
        }
        return DUMP_DIR;
    }

    static Path getMarkerPath() {
        if (MARKER_FILE == null) {
            MARKER_FILE = getCriuPath().resolve("wildfly-checkpoint.txt");
        }
        return MARKER_FILE;
    }
}
