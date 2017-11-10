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

import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;

/**
 * Wraps a legacy API {@code MinMaxValidator} in an object that exposes the modern API. This allows
 * the legacy validator to be used in an {@link org.wildfly.management.api.model.definition.ItemDefinition}.
 *
 * @author Brian Stansberry
 */
public final class LegacyToModernMinMaxValidator extends LegacyToModernParameterValidator implements org.wildfly.management.api.model.validation.MinMaxValidator {

    private final MinMaxValidator wrapped;

    public LegacyToModernMinMaxValidator(ParameterValidator legacy) {
        super(legacy);
        wrapped = legacy instanceof MinMaxValidator ? (MinMaxValidator) legacy : null;
    }

    @Override
    public Long getMin() {
        return wrapped == null ? null : wrapped.getMin();
    }

    @Override
    public Long getMax() {
        return wrapped == null ? null : wrapped.getMax();
    }
}
