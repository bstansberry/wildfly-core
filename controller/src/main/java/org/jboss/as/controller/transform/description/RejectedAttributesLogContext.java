/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.transform.description;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class RejectedAttributesLogContext {

    private final TransformationRule.AbstractChainedContext context;
    private final PathAddress address;
    private final ModelNode op;
    Map<String, RejectAttributeChecker> failedCheckers;
    Map<String, Map<String, ModelNode>> failedAttributes;

    RejectedAttributesLogContext(TransformationRule.AbstractChainedContext context, PathAddress address, ModelNode op) {
        this.context = context;
        this.address = address;
        this.op = op;
    }

    void checkAttribute(RejectAttributeChecker checker, String attributeName, ModelNode attributeValue) {
        if (op == null) {
            if (checker.rejectResourceAttribute(address, attributeName, attributeValue, context.getContext())) {
                reject(checker, attributeName, attributeValue);
            }
        } else {
            if (checker.rejectOperationParameter(address, attributeName, attributeValue, op, context.getContext())){
                reject(checker, attributeName, attributeValue);
            }
        }
    }

    private void reject(RejectAttributeChecker checker, String attributeName, ModelNode attributeValue) {
        assert checker.getRejectionLogMessageId() != null : "Null log id";
        final String id = checker.getRejectionLogMessageId();
        if (failedCheckers == null) {
            failedCheckers = new HashMap<String, RejectAttributeChecker>();
        }
        if (failedCheckers.get(id) == null) {
            failedCheckers.put(id, checker);
        }

        if (failedAttributes == null) {
            failedAttributes = new LinkedHashMap<String, Map<String, ModelNode>>();
        }
        Map<String, ModelNode> attributes = failedAttributes.get(checker.getRejectionLogMessageId());
        if (attributes == null) {
            attributes = new HashMap<String, ModelNode>();
            failedAttributes.put(checker.getRejectionLogMessageId(), attributes);
        }
        attributes.put(attributeName, attributeValue);
    }

    boolean hasRejections() {
        return failedAttributes != null;
    }

    String errorOrWarnOnResourceTransformation() throws OperationFailedException {
        if (op != null) {
            throw new IllegalStateException();
        }
        if (failedAttributes == null) {
            return "";
        }

        final TransformationTarget tgt = context.getContext().getTarget();
        final String legacyHostName = tgt.getHostName();
        final ModelVersion coreVersion = tgt.getVersion();
        final String subsystemName = findSubsystemName(address);
        final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

        final TransformersLogger logger = context.getContext().getLogger();
        final boolean error = tgt.isIgnoredResourceListAvailableAtRegistration();
        List<String> messages = error ? new ArrayList<String>() : null;

        for (Map.Entry<String, Map<String, ModelNode>> entry : failedAttributes.entrySet()) {
            RejectAttributeChecker checker = failedCheckers.get(entry.getKey());
            String message = checker.getRejectionLogMessage(entry.getValue());

            if (error) {
                //Create our own custom exception containing everything
                messages.add(message);
            } else {
                return logger.getAttributeWarning(address, op, message, entry.getValue().keySet());
            }
        }

        if (error) {
            // Target is  7.2.x or higher so we should throw an error
            if (subsystemName != null) {
                throw ControllerLogger.ROOT_LOGGER.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, messages);
            }
            throw ControllerLogger.ROOT_LOGGER.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, messages);
        }
        return null;
    }

    //todo replace with context.getLogger()....
    String getOperationRejectDescription() {
        if (op == null) {
            throw new IllegalStateException();
        }
        if (failedAttributes == null) {
            return "";
        }

        final TransformersLogger logger = context.getContext().getLogger();

        for (Map.Entry<String, Map<String, ModelNode>> entry : failedAttributes.entrySet()) {
            RejectAttributeChecker checker = failedCheckers.get(entry.getKey());
            String message = checker.getRejectionLogMessage(entry.getValue());

            return logger.getAttributeWarning(address, op, message, entry.getValue().keySet());
        }
        return null;
    }

    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }
}
