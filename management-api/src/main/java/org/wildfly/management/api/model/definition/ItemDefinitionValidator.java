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

package org.wildfly.management.api.model.definition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.management.api.OperationFailedException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;
import org.wildfly.management.api.model.validation.ModelTypeValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Performs validation of an {@link ItemDefinition}.
 *
 * @author Brian Stansberry
 */
final class ItemDefinitionValidator {

    private ItemDefinitionValidator() {
        // prevent instantiation
    }

    static void validateItem(ItemDefinition itemDefinition, ModelNode value) throws OperationFailedException {
        switch (value.getType()) {
            case EXPRESSION:
                if (!itemDefinition.isAllowExpression()) {
                    throw ControllerLoggerDuplicate.ROOT_LOGGER.expressionNotAllowed(itemDefinition.getName());
                }
                break;
            case UNDEFINED:
                if (!itemDefinition.isNillable()) {
                    throw ControllerLoggerDuplicate.ROOT_LOGGER.nullNotAllowed(itemDefinition.getName());
                }
                break;
            default: {
                ParameterValidator configuredValidator = itemDefinition.getValidator();
                String itemName = itemDefinition.getName();
                if (configuredValidator == null || !configuredValidator.replacesDefaultValidation()) {
                    performStandardCheck(itemDefinition, value, itemName);
                }
                if (configuredValidator != null) {
                    configuredValidator.validateParameter(itemName, value);
                }
            }
        }
    }

    private static void performStandardCheck(ItemDefinition itemDefinition, ModelNode value, String itemName) throws OperationFailedException {
        ModelType type = itemDefinition.getType();
        // First, perform a simple type check
        ModelTypeValidator.validateType(itemName, value, type, false);

        if (itemDefinition instanceof CollectionItemDefinition) {
            validateCollectionType((CollectionItemDefinition) itemDefinition, value, itemName, type);
        } else if (itemDefinition instanceof ObjectTypeItemDefinition) {
            validateObjectType((ObjectTypeItemDefinition) itemDefinition, value);
        } else {
            assert itemDefinition instanceof SimpleItemDefinition; // or we added something new we need to handle
            // Check range or length
            validateMinMax(itemDefinition, value, type);
            // Finally, allowed values
            validateAllowedValues(itemDefinition, value);
        }
    }

    private static void validateCollectionType(CollectionItemDefinition cid, ModelNode value, String itemName, ModelType type) throws OperationFailedException {
        // Check size
        validateMinMax(cid, value, type);

        ItemDefinition elementDefinition = cid.getElementDefinition();
        if (type == ModelType.LIST) {
            boolean allowDups = cid.isAllowDuplicates();
            Set<ModelNode> seen = allowDups ? null : new HashSet<>();
            for (ModelNode element : value.asList()) {
                validateItem(elementDefinition, element);
                if (!allowDups && !seen.add(element)) {
                    throw ControllerLoggerDuplicate.ROOT_LOGGER.duplicateElementsInList(itemName);
                }
            }
        } else {
            for (Property property : value.asPropertyList()) {
                validateItem(elementDefinition, property.getValue());
            }
        }
    }

    private static void validateObjectType(ObjectTypeItemDefinition itemDefinition, ModelNode value) throws OperationFailedException {
        for (ItemDefinition ad : itemDefinition.getValueTypes()) {
            String key = ad.getName();
            // Don't modify the value by calls to get(), because that's best in general.
            // Plus modifying it results in an irrelevant test failure in full where the test
            // isn't expecting the modification and complains.
            // Changing the test is too much trouble.
            ModelNode toTest = value.has(key) ? value.get(key) : new ModelNode();
            validateItem(ad, toTest);
        }
    }

    private static void validateMinMax(ItemDefinition itemDefinition, ModelNode value, ModelType type) throws OperationFailedException {
        Long minL = itemDefinition.getMin();
        Long maxL = itemDefinition.getMax();
        if (minL != null || maxL != null) {
            long min = minL != null ? minL : Long.MIN_VALUE;
            long max = maxL != null ? maxL : Long.MAX_VALUE;
            int minI = (int) min;
            int maxI = (int) max;
            int length;
            switch (type) {
                case STRING: {
                    // Length check
                    String str = value.asString();
                    length = str.length();
                    if (length < minI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinLength(str, itemDefinition.getName(), minI));
                    } else if (length > maxI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxLength(str, itemDefinition.getName(), maxI));
                    }
                    break;
                }
                case BYTES: {
                    // Length check
                    length = value.asBytes().length;
                    if (length < minI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinSize(length, itemDefinition.getName(), minI));
                    } else if (length > maxI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxSize(length, itemDefinition.getName(), maxI));
                    }
                    break;
                }
                case INT:
                case LIST:
                case OBJECT: {
                    // int range check or collection size check
                    int val = value.asInt(); // asInt returns size for LIST or OBJECT
                    if (val < minI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinValue(val, itemDefinition.getName(), minI));
                    } else if (val > maxI) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxValue(val, itemDefinition.getName(), maxI));
                    }
                    break;
                }
                case LONG:
                case DOUBLE:{
                    // long range check
                    long val = value.asLong();
                    if (val < min) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinValue(val, itemDefinition.getName(), min));
                    } else if (val > max) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxValue(val, itemDefinition.getName(), max));
                    }
                    break;
                }
                case BIG_DECIMAL:
                case BIG_INTEGER: {
                    // long range check but only if explicitly configured
                    long val = value.asLong();
                    if (minL != null && val < min) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinValue(val, itemDefinition.getName(), min));
                    } else if (maxL != null && val > max) {
                        throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxValue(val, itemDefinition.getName(), max));
                    }
                    break;
                }
            }
        }
    }

    private static void validateAllowedValues(ItemDefinition itemDefinition, ModelNode value) throws OperationFailedException {
        List<ModelNode> allowed = itemDefinition.getAllowedValues();
        if (!allowed.isEmpty() && !allowed.contains(value)) {
            throw ControllerLoggerDuplicate.ROOT_LOGGER.invalidValue(value.asString(), itemDefinition.getName(), allowed);
        }
    }
}
