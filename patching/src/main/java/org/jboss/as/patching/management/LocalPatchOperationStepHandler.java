/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.management;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchOperationTarget;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.repository.ContentReference;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Emanuel Muckenhuber
 */
public final class LocalPatchOperationStepHandler implements OperationStepHandler {
    public static final OperationStepHandler INSTANCE = new LocalPatchOperationStepHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // Acquire the lock and check the write permissions for this operation
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final InstallationManager installationManager = (InstallationManager) registry.getRequiredService(InstallationManagerService.NAME).getValue();

        if (installationManager.requiresRestart()) {
            throw PatchLogger.ROOT_LOGGER.serverRequiresRestart();
        }

        ContentRepository contentRepository = null;
        ContentReference contentReference = null;
        try {
            final PatchTool runner = PatchTool.Factory.create(installationManager);
            final ContentVerificationPolicy policy = PatchTool.Factory.create(operation);

            final InputStream is;
            if (operation.hasDefined(PatchResourceDefinition.CONTENT_HASH.getName())) {
                byte[] hash = PatchResourceDefinition.CONTENT_HASH.resolveModelAttribute(context, operation).asBytes();
                contentRepository = (ContentRepository) registry.getRequiredService(ContentRepository.SERVICE_NAME).getValue();
                contentReference = new ContentReference("_patch_" + System.currentTimeMillis(), hash);
                if (!contentRepository.syncContent(contentReference)) {
                    throw PatchLogger.ROOT_LOGGER.noPatchFoundWithHash(HashUtil.bytesToHexString(hash));
                }
                contentRepository.addContentReference(contentReference);
                try {
                    is = contentRepository.getContent(hash).openStream();
                } catch (IOException e) {
                    throw new PatchingException(e);
                }
            } else {
                final int index = operation.get(PatchResourceDefinition.INPUT_STREAM_IDX_DEF.getName()).asInt(0);
                is = context.getAttachmentStream(index);
            }
            installationManager.restartRequired();
            final PatchingResult result;
            try {
                result = runner.applyPatch(is, policy);
            } finally {
                if (contentReference != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                        // TODO log a WARN
                    }
                }
            }
            context.restartRequired();
            context.completeStep(new OperationContext.ResultHandler() {

                @Override
                public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                    if(resultAction == OperationContext.ResultAction.KEEP) {
                        result.commit();
                    } else {
                        installationManager.clearRestartRequired();
                        context.revertRestartRequired();
                        result.rollback();
                    }
                }

            });
        } catch (PatchingException e) {
            final ModelNode failureDescription = context.getFailureDescription();
            PatchOperationTarget.formatFailedResponse(e, failureDescription);
            installationManager.clearRestartRequired();
        } finally {
            if (contentReference != null && contentRepository != null) {
                // No matter what the result was (which we don't even know at this point),
                // we only leave the content in the repo for a single invocation of this op.
                contentRepository.removeContent(contentReference);
            }
        }
    }

}
