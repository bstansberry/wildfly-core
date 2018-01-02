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

package org.wildfly.management.api.model.validation;

import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api.model.Resource;

/**
 * Performs custom validation of a resource or of its use with an overall configuration model, throwing an
 * {@link OperationClientException} if issues are found. Should not be used for validation of individual attributes,
 * or of relationships between attributes that can be described via an
 * {@link org.wildfly.management.api.model.definition.ItemDefinition} (e.g.
 * {@link org.wildfly.management.api.model.definition.ItemDefinition.Builder#setAlternatives(String...) alternatives}
 * or
 * {@link org.wildfly.management.api.model.definition.ItemDefinition.Builder#setRequires(String...) requires}.
 * The kernel will automatically provide that kind of validation; use this for custom validation that cannot be
 * expressed declaratively.
 * <p>
 * The validator will be called as part of any operation that modifies a resource of the type with which the
 * validator has been
 * {@link org.wildfly.management.api.model.definition.ResourceTypeDefinition.Builder#setValidator(ResourceValidator) associated},
 * with the validation occurring before operation execution proceeds to the stage where modifications of the process
 * runtime are performed. {@link ParameterValidator Validation of individual parameters} used to update the resource will
 * have been performed, but automatic kernel checks of things like {@code alternatives} or {@code requires} or of
 * capability references may not have been performed yet.
 *
 * @author Brian Stansberry
 */
@FunctionalInterface
public interface ResourceValidator {

    /**
     * Validate the resource, throwing an {@link OperationClientException} if issues are found.
     * @param resource the resource to validate. Will not be {@code null}
     * @param context contextual object to use when performing the validation. Will not be {@code null}
     * @throws OperationClientException if a validation issue is discovered.
     */
    void validateResource(Resource resource, ResourceValidationContext context) throws OperationClientException;

    /**
     * Contextual object that provides a {@link ResourceValidator} with access to information and
     * functionality needed to perform a validation.
     */
    interface ResourceValidationContext {
        // TODO
    }
}
