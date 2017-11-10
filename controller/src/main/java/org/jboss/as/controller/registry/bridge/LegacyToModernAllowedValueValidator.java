/*
Copyright 2018 Red Hat, Inc.

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

import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;

/**
 * Wraps a legacy API {@code AllowedValuesValidator} in an object that exposes the modern API. This allows
 * the legacy validator to be used in an {@link org.wildfly.management.api.model.definition.ItemDefinition}.
 *
 * @author Brian Stansberry
 */
public class LegacyToModernAllowedValueValidator extends LegacyToModernParameterValidator implements org.wildfly.management.api.model.validation.AllowedValuesValidator {

    private final AllowedValuesValidator wrapped;

    public LegacyToModernAllowedValueValidator(ParameterValidator legacy) {
        super(legacy);
        wrapped = legacy instanceof AllowedValuesValidator ? (AllowedValuesValidator) legacy : null;
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return wrapped == null ? null : wrapped.getAllowedValues();
    }
}
