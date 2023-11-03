/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.nio.file.Path;

import org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller._private.OperationFailedRuntimeException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Internationalized messages for CRIU integration.
 *
 * @author <a href="mailto:brian.stansberry@redhat.com">Brian Stansberry</a>
 */
@MessageLogger(projectCode = "WFCRIU", length = 5)
interface Messages extends BasicLogger {

    Messages ROOT_LOGGER = Logger.getMessageLogger(Messages.class, "org.wildfly.core.criu");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Beginning preparation for a JVM checkpoint.")
    void beforeCheckpointBegin();

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Completed preparation for a JVM checkpoint in %d ms.")
    void beforeCheckpointEnd(long time);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Beginning restoration following a JVM restore.")
    void afterRestoreBegin();

    @LogMessage(level = INFO)
    @Message(id = 4, value = "Completed restoration following a JVM restore.")
    void afterRestoreEnd();

    @Message(id = 5, value = "Cannot prepare for a checkpoint, as an incomplete checkpoint preparation already exists.")
    IllegalStateException checkpointPreparationAlreadyInProgress();

    @Message(id = 6, value = "Cannot prepare for a checkpoint, as no reload initiator has been registered.")
    IllegalStateException checkpointPreparationWithNoInitiator();

    @LogMessage(level = WARN)
    @Message(id = 7, value = "No in-progress reload is available to continue after restoring a checkpoint.")
    void checkpointRestorationWithNoPreparation();

    @LogMessage(level = ERROR)
    @Message(id = 8, value = "The reload operation was interrupted after signalling ready for checkpoint but " +
            "before an after-restoration signal was received.")
    void interruptedAwaitingCheckpointRestoration();

    @Message(id = 9, value = "Call to suspend to prepare for a JVM checkpoint failed -- %s")
    RuntimeException checkpointPreparationSuspendFailed(String failureInfo);

    @Message(id = 10, value = "Call to reload to prepare for a JVM checkpoint failed -- %s")
    RuntimeException checkpointPreparationReloadFailed(String failureInfo);

    @Message(id = 11, value = "Cannot checkpoint -- no JVM CRIU implementation is available")
    OperationFailedException checkpointNotSupported();

    @LogMessage(level = INFO)
    @Message(id = 12, value = "%s is available and enabled; a functional JVM checkpointing integration will be installed.")
    void criuImplementationEnabled(String integrationType);

    @LogMessage(level = DEBUG) // perhaps INFO but this is noise logging in the 99% case
    @Message(id = 13, value = "%s JVM checkpointing is available but disabled. %s")
    void criuImplementationDisabled(String integrationType, String moreInfo);
    @LogMessage(level = ERROR)
    @Message(id = 14, value = "%s is available but integration failed")
    void criuIntegrationFailed(String integrationType, @Cause Throwable t);

    @LogMessage(level = INFO)
    @Message(id = 15, value = "ModelController has signalled it is ready for a JVM checkpoint.")
    void readyForCheckpoint();

    @Message(id = 16, value = "Call to resume server after restoring from a JVM checkpoint failed -- %s")
    RuntimeException checkpointRestoreResumeFailed(String failureInfo);

    @Message(id = 17, value = "Checkpoint strategy %s is not supported in this server")
    OperationFailedRuntimeException checkpointStrategyNotSupported(CheckpointStrategy strategy);

    @LogMessage(level = INFO)
    @Message(id = 18, value = "Directory %s exists  and cannot be emptied. JVM checkpointing is available but disabled.")
    void checkpointDirAlreadyExists(Path checkpointDir);

}
