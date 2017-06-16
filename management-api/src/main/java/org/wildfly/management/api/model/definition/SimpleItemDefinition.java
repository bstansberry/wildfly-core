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

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

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
         * Sets allowed values for the item.
         *
         * @param allowedValues values that are legal for this item
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setAllowedValues(String ... allowedValues) {
            return super.setAllowedValues(allowedValues);
        }

        /**
         * Sets allowed values for the item
         *
         * @param allowedValues values that are legal for this item
         * @return a builder that can be used to continue building the item definition
         */
        @Override
        public Builder setAllowedValues(int ... allowedValues) {
            return super.setAllowedValues(allowedValues);
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
