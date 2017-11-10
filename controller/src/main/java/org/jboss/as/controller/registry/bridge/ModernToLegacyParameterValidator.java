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

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.model.definition.ItemDefinition;
import org.wildfly.management.api.model.definition.ItemDefinitionValidator;

/**
 * Wraps a current API {@code ItemDefinition} in an object that exposes the legacy {@link ParameterValidator} API.
 * This solely exists to support the public {@link AttributeDefinition#getValidator()} contract.
 *
 * @author Brian Stansberry
 */
public final class ModernToLegacyParameterValidator implements ParameterValidator, MinMaxValidator, AllowedValuesValidator {

    private final ItemDefinition itemDefinition;

    public ModernToLegacyParameterValidator(ItemDefinition itemDefinition) {
        this.itemDefinition = itemDefinition;
    }

    /**
     * Simply invokes the wrapped validator, throwing the unchecked {@link org.wildfly.management.api.OperationClientException}
     * if the value is not valid.
     *
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @param value the parameter value. Cannot be {@code null}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) {
        ItemDefinitionValidator.validateItem(itemDefinition, value);
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        List<ModelNode> modern = itemDefinition.getAllowedValues();
        return modern.isEmpty() ? null : modern;
    }

    @Override
    public Long getMin() {
        return itemDefinition.getMin();
    }

    @Override
    public Long getMax() {
        return itemDefinition.getMax();
    }
}
