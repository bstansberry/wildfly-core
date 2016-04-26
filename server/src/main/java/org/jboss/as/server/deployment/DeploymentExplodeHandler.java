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

package org.jboss.as.server.deployment;

import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_ARCHIVE;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.CONTENT_HASH;
import static org.jboss.as.server.controller.resources.DeploymentAttributes.ENABLED;
import static org.jboss.as.server.deployment.DeploymentHandlerUtil.getContentItem;
import static org.jboss.as.server.deployment.DeploymentHandlerUtil.isArchive;
import static org.jboss.as.server.deployment.DeploymentHandlerUtil.isManaged;

import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.ExplodedContentException;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the "explode" operation that can be performed against zipped managed content.
 *
 * @author Brian Stansberry
 */
public class DeploymentExplodeHandler implements OperationStepHandler {


    private final ContentRepository contentRepository;

    public DeploymentExplodeHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.getProcessType() == ProcessType.SELF_CONTAINED) {
            // TODO i18n
            throw new OperationFailedException("Cannot explode deployment content in a self-contained server");
        }

        Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        ModelNode contentItem = getContentItem(resource);

        // Validate this op is available
        if (!isManaged(contentItem)) {
            // TODO i18n
            throw new OperationFailedException("Cannot explode unmanaged deployment content");
        } else if (!isArchive(contentItem)) {
            // TODO i18n
            throw new OperationFailedException("Cannot explode already exploded deployment content");
        }
        ModelNode model = resource.getModel();
        if (context.isNormalServer()) {
            boolean enabled = ENABLED.resolveModelAttribute(context, model).asBoolean();
            if (enabled) {
                // TODO i18n
                throw new OperationFailedException("Cannot explode currently deployed deployment content");
            }
        }

        byte[] oldHash = CONTENT_HASH.resolveModelAttribute(context, contentItem).asBytes();
        byte[] newHash;
        try {
            newHash = contentRepository.explodeContent(oldHash);
        } catch (ExplodedContentException e) {
            throw new OperationFailedException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        contentItem.get(CONTENT_HASH.getName()).set(newHash);
        contentItem.get(CONTENT_ARCHIVE.getName()).set(false);

        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction == OperationContext.ResultAction.KEEP) {
                    PathAddress address = context.getCurrentAddress();
                    contentRepository.removeContent(ModelContentReference.fromModelAddress(address, oldHash));
                    contentRepository.addContentReference(ModelContentReference.fromModelAddress(context.getCurrentAddress(), newHash));
                } // else the model update will be reverted and no ref content repo references changes will be made so
                  // the newly exploded content will be eligible for content repo gc
            }
        });
    }
}
