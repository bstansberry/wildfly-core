/*
Copyright 2017 Red Hat, Inc.

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

package org.jboss.as.controller.registry.bridge;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Wraps a legacy API {@code ParameterValidator} in an object that exposes the modern API. This allows
 * the legacy validator to be used in an {@link org.wildfly.management.api.model.definition.ItemDefinition}.
 *
 * @author Brian Stansberry
 */
public class LegacyToModernParameterValidator implements ParameterValidator {

    private final org.jboss.as.controller.operations.validation.ParameterValidator legacy;

    public LegacyToModernParameterValidator(org.jboss.as.controller.operations.validation.ParameterValidator legacy) {
        this.legacy = legacy;
    }

    @Override
    public final void validateParameter(String parameterName, ModelNode value) throws OperationClientException {
        try {
            legacy.validateParameter(parameterName, value);
        } catch (org.jboss.as.controller.OperationFailedException e) {
            throw e.toModernForm();
        }
    }

    @Override
    public boolean replacesDefaultValidation() {
        return true;
    }
}
