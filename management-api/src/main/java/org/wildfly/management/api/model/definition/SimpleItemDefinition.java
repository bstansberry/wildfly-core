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

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.model.validation.AllowedValuesValidator;
import org.wildfly.management.api.model.validation.MinMaxValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * {@link ItemDefinition} for an item whose {@link #getType() type} is a simple {@link org.jboss.dmr.ModelType},
 * not a {@link org.jboss.dmr.ModelType#LIST}, {@link org.jboss.dmr.ModelType#OBJECT}
 * or {@link org.jboss.dmr.ModelType#PROPERTY}.
 *
 * @author Brian Stansberry
 */
public final class SimpleItemDefinition extends ItemDefinition {

    private SimpleItemDefinition(Builder builder) {
        super(builder);
    }

    /** Builder for a {@link SimpleItemDefinition}. */
    public static final class Builder extends ItemDefinition.Builder<Builder, SimpleItemDefinition> {

        /**
         * Creates a builder for a simple {@link ItemDefinition}.
         * @param name the name of the item. Cannot be {@code null}
         * @param type the type of the item. Cannot be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public static Builder of(String name, ModelType type) {
            return new Builder(name, type);
        }

        /**
         * Creates a builder for a simple {@link ItemDefinition} where the item has no name. This is
         * useful for defining items that are elements in a {@link CollectionItemDefinition}.
         *
         * @param type the type of the item. Cannot be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public static Builder of(ModelType type) {
            return new Builder("", type);
        }

        /**
         * Creates a builder for a simple {@link ItemDefinition} whose initial settings will be based on
         * an existing item definition.
         * @param basis the existing item definition. Cannot be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public static Builder of(ItemDefinition basis) {
            return new Builder(null, basis);
        }

        /**
         * Creates a builder for a simple {@link ItemDefinition} whose initial settings, other than the item name,
         * will be based on an existing item definition.
         * @param name the name of the new item. Cannot be {@code null}
         * @param basis the existing item definition. Cannot be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public static Builder of(String name, ItemDefinition basis) {
            return new Builder(name, basis);
        }

        Builder(String itemName, ModelType type) {
            super(itemName, type);
        }

        Builder(String name, ItemDefinition basis) {
            super(name, basis);
        }

        @Override
        public SimpleItemDefinition build() {
            return new SimpleItemDefinition(this);
        }

        /**
         * Sets a minimum value for a numeric type. Not relevant for non-numeric types.
         * @param min the mimimum value
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setMin(long min) {
            return super.setMin(min);
        }

        /**
         * Sets a maximum value for a numeric type. Not relevant for non-numeric types.
         * @param max the mimimum value
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setMax(long max) {
            return super.setMax(max);
        }

        /**
         * Sets whether the item should {@link ItemDefinition#isAllowExpression() allow expressions}
         * If not set the default value is {@code false}.
         *
         * @param allowExpression {@code true} if expression values should be allowed
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setAllowExpression(boolean allowExpression) {
            return super.setAllowExpression(allowExpression);
        }

        /**
         * Sets allowed values for the item.
         *
         * @param allowedValues values that are legal for this item
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setAllowedValues(ModelNode... allowedValues) {
            return super.setAllowedValues(allowedValues);
        }

        /**
         * Sets allowed values for the item. This is a convenience method that calls
         * {@link #setAllowedValues(ModelNode...)} after creating a {@code ModelNode} array the values of whose nodes
         * match the provided {@code allowedValues}.
         *
         * @param allowedValues values that are legal for this item
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setAllowedValues(String ... allowedValues) {
            assert allowedValues!= null;
            ModelNode[] array = new ModelNode[allowedValues.length];
            for (int i = 0; i < allowedValues.length; i++) {
                array[i] = new ModelNode(allowedValues[i]);
            }
            return super.setAllowedValues(array);
        }

        /**
         * Sets allowed values for the item. This is a convenience method that calls
         * {@link #setAllowedValues(ModelNode...)} after creating a {@code ModelNode} array the values of whose nodes
         * match the provided {@code allowedValues}.
         *
         * @param allowedValues values that are legal for this item
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setAllowedValues(int ... allowedValues) {
            assert allowedValues!= null;
            ModelNode[] array = new ModelNode[allowedValues.length];
            for (int i = 0; i < allowedValues.length; i++) {
                array[i] = new ModelNode(allowedValues[i]);
            }
            return super.setAllowedValues(array);
        }

        /**
         * Sets allowed values for the item. This is a convenience method that calls {@link #setAllowedValues(ModelNode...)}
         * after creating a {@code ModelNode} array the values of whose nodes match the provided {@code allowedValues}.
         *
         * @param allowedValues values that are legal for this item
         * @param <E> the type of the enum
         * @return a builder that can be used to continue building the item definition
         */
        @SafeVarargs
        public final <E extends Enum<E>> Builder setAllowedValues(final E... allowedValues) {
            return setValidator(EnumValidator.create(allowedValues));
        }

        /**
         * Sets allowed values for the item to all of the values of the given {@code enumType}.
         * This is a convenience method that calls {@link #setAllowedValues(ModelNode...)}
         * after creating a {@code ModelNode} array the values of whose nodes match the provided {@code allowedValues}.
         *
         * @param enumType the type of the enum
         * @param <E> the type of the enum
         * @return a builder that can be used to continue building the item definition
         */
        public <E extends Enum<E>> Builder setAllowedValues(final Class<E> enumType) {
            return setValidator(EnumValidator.create(enumType));
        }

        /**
         * Sets allowed values for the item to set of the values of the given {@code enumType}.
         * This is a convenience method that calls {@link #setAllowedValues(ModelNode...)}
         * after creating a {@code ModelNode} array the values of whose nodes match the provided {@code allowedValues}.
         *
         * @param enumType the type of the enum
         * @param allowedValues values that are legal for this item
         * @param <E> the type of the enum
         * @return a builder that can be used to continue building the item definition
         */
        public <E extends Enum<E>> Builder setAllowedValues(final Class<E> enumType, final EnumSet<E> allowedValues) {
            return setValidator(EnumValidator.create(enumType, allowedValues));
        }

        /**
         * Sets the validator that should be used to validate item values. This is only relevant to operation parameter
         * use cases. The item definition produced by this builder will directly enforce the item's
         * {@link ItemDefinition#isRequired()} () required} and
         * {@link ItemDefinition#isAllowExpression() allow expression} settings, so the given {@code validator}
         * need not concern itself with those validations.
         * <p>
         * As a convenience, this method checks if {@code validator} implements
         * {@link AllowedValuesValidator} or {@link MinMaxValidator} and if so calls {@link #setMin(long)},
         * {@link #setMax(long)} or {@link #setAllowedValues(ModelNode...)} as appropriate.
         * <p>
         * <strong>Usage note:</strong> Providing a validator should be limited to atypical custom validation cases.
         * Standard validation against the item's definition (i.e. checking for correct type, value or size within
         * min and max, or adherence to a fixed set of allowed values) will be automatically handled without requiring
         * provision of a validator.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setValidator(ParameterValidator validator) {
            if (validator instanceof AllowedValuesValidator) {
                List<ModelNode> allowed = ((AllowedValuesValidator) validator).getAllowedValues();
                setAllowedValues(allowed.toArray(new ModelNode[allowed.size()]));
            }
            if (validator instanceof MinMaxValidator) {
                MinMaxValidator mmv = (MinMaxValidator) validator;
                setMin(mmv.getMin());
                setMin(mmv.getMax());
            }
            return super.setValidator(validator);
        }

        /**
         * Sets a {@link ItemDefinition#getMeasurementUnit() measurement unit} to describe the unit in
         * which a numeric item is expressed.
         * @param unit the unit. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setMeasurementUnit(MeasurementUnit unit) {
            return super.setMeasurementUnit(unit);
        }

        /**
         * Sets a custom {@link AttributeMarshaller} to use for marshalling the item to xml.
         * If not set, {@link AttributeMarshaller#SIMPLE} will be used.
         * @param marshaller the marshaller. Can be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setAttributeMarshaller(AttributeMarshaller marshaller) {
            return super.setAttributeMarshaller(marshaller);
        }

        /**
         * Sets a custom {@link AttributeParser} to use for parsing the item's value from xml.
         * If not set, {@link AttributeParser#SIMPLE} will be used.
         * @param parser the parser. Can be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setAttributeParser(AttributeParser parser) {
            return super.setAttributeParser(parser);
        }
    }
}
