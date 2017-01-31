/*
 Copyright 2017, Red Hat, Inc.

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
package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for the {@code read-attribute-description} handler.
 *
 * @author Brian Stansberry
 */
public class ReadAttrbuteDescriptionHandler implements OperationStepHandler {

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_ATTRIBUTE_DESCRIPTION_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(GlobalOperationAttributes.NAME, GlobalOperationAttributes.LOCALE)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .build();

    /**
     * Transformer for this operation for slave Host Controllers running versions prior to
     * WildFly Core 3.0 (i.e. AS 7, EAP 6, WildFly 8, 9, 10 an EAP 7.0).
     */
    public static final OperationTransformer TRANSFORMER = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            ModelNode transformedOp = createRRDOperation(address, operation);
            ResultTransformer rt = new ResultTransformer(operation);
            return new TransformedOperation(transformedOp, rt);
        }
    };

    static final OperationStepHandler INSTANCE = new ReadAttrbuteDescriptionHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Add a step to the top of the queue do a read-resource-description,
        // writing its output to a response node we create.
        // Then in a ResultHandler, which will execute after the r-r-d step,
        // extract the attribute description from that response node.
        final ModelNode rrdResponse = new ModelNode();
        final String attributeName = GlobalOperationAttributes.NAME.resolveModelAttribute(context, operation).asString();

        final PathAddress addr = context.getCurrentAddress();
        final OperationStepHandler rrdHandler = context.getRootResourceRegistration().getOperationHandler(addr, READ_RESOURCE_DESCRIPTION_OPERATION);
        final ModelNode rrdOp = createRRDOperation(addr, operation);

        context.addModelStep(rrdResponse, rrdOp, ReadResourceDescriptionHandler.DEFINITION, rrdHandler, true);

        context.completeStep((resultAction, rhContext, rhOperation) -> {
            ModelNode radResponse = convertRRDResponse(rrdResponse, attributeName);
            if (radResponse.hasDefined(FAILURE_DESCRIPTION)) {
                if (!rhContext.hasFailureDescription()) {
                    context.getFailureDescription().set(radResponse.get(FAILURE_DESCRIPTION));
                }
            } else {
                context.getResult().set(radResponse.get(RESULT));
            }
        });
    }

    private static ModelNode createRRDOperation(PathAddress address, ModelNode baseOperation) {
        final ModelNode rrdOp = Util.createEmptyOperation(READ_RESOURCE_DESCRIPTION_OPERATION, address);
        rrdOp.get(RECURSIVE).set(false);
        rrdOp.get(ACCESS_CONTROL).set(ReadResourceDescriptionHandler.AccessControl.COMBINED_DESCRIPTIONS.toString());
        rrdOp.get(INHERITED).set(false);
        if (baseOperation.hasDefined(LOCALE)) {
            rrdOp.get(LOCALE).set(baseOperation.get(LOCALE));
        }
        return rrdOp;
    }

    private static ModelNode convertRRDResponse(ModelNode rrdResponse, String attributeName) {
        ModelNode radResponse = new ModelNode();

        if (rrdResponse.hasDefined(FAILURE_DESCRIPTION)) {
            radResponse = rrdResponse.clone();
            radResponse.remove(RESULT);
        } else if (rrdResponse.hasDefined(RESULT)) {
            ModelNode radResult = convertRRDResult(rrdResponse.get(RESULT), attributeName);
            if (radResult.isDefined()) {
                radResponse = rrdResponse.clone();
                radResponse.get(RESULT).set(radResult);
            } // else there was no matching attribute data, so fall through
        }

        if (!radResponse.isDefined()) {
            radResponse.get(OUTCOME).set(FAILED);
            radResponse.get(FAILURE_DESCRIPTION).set(ControllerLogger.ROOT_LOGGER.unknownAttribute(attributeName));
        }
        return radResponse;
    }

    private static ModelNode convertRRDResult(ModelNode rrdResult, String attributeName) {
        ModelNode radResult = new ModelNode();

        if (rrdResult.getType() == ModelType.LIST) {
            // Wildcard request.
            // To be consistent with read-attribute, any wildcard request gets a response
            // even if no resource had that attribute defined. Response is an empty list.
            // So, first establish that list
            radResult.setEmptyList();
            // Now add entries for any resources with the attribute described
            for (ModelNode rrdItem : rrdResult.asList()) {
                if (rrdItem.hasDefined(RESULT, ATTRIBUTES, attributeName)) {
                    ModelNode radItem = rrdItem.clone();
                    radItem.get(RESULT).set(rrdItem.get(RESULT, ATTRIBUTES, attributeName));
                    radResult.add(radItem);
                }
            }
        } else {
            if (rrdResult.hasDefined(ATTRIBUTES, attributeName)) {
                radResult.set(rrdResult.get(ATTRIBUTES, attributeName));
            }
        }
        return radResult;
    }

    private static class ResultTransformer implements OperationResultTransformer {

        private final String attributeName;

        private ResultTransformer(ModelNode operation) {
            this.attributeName = operation.hasDefined(NAME) ? operation.get(NAME).asString() : null;
        }

        @Override
        public ModelNode transformResult(ModelNode response) {

            ModelNode transformedResponse;
            if (attributeName == null) {
                // Request was bogus; change the response to what it would have been if we'd caught that
                transformedResponse = new ModelNode();
                transformedResponse.get(OUTCOME).set(FAILED);
                OperationFailedException ofe = ControllerLogger.ROOT_LOGGER.nullNotAllowed(NAME);
                transformedResponse.get(FAILURE_DESCRIPTION).set(ofe.getFailureDescription());
            } else {
                transformedResponse = convertRRDResponse(response, attributeName);
            }
            return transformedResponse;
        }
    }
}
