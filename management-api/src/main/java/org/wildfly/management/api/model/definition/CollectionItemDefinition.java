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

import org.jboss.dmr.ModelType;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Definition of an item that consists of a collection of other items.
 *
 * @param <E> the type of {@link ItemDefinition} used for {@link #getElementDefinition() collection elements}
 * @author Brian Stansberry
 */
public abstract class CollectionItemDefinition<E extends ItemDefinition> extends ItemDefinition {

    private final E elementDefinition;

    CollectionItemDefinition(CollectionItemDefinition.Builder<?, ?, E> toCopy) {
        super(toCopy);
        this.elementDefinition = toCopy.getElementDefinition();
    }

    /**
     * Gets the definition for elements in the collection. For items of {@link ModelType#LIST}
     * this will be the list elements; for items of type {@link ModelType#OBJECT} it will be the values
     * stored under each key.
     *
     * @return the element definition. Will not return {@code null}
     */
    public final E getElementDefinition() {
        return elementDefinition;
    }

    public boolean isAllowDuplicates() {
        return false;
    }

    /** Builder for creating a {@link CollectionItemDefinition} */
    abstract static class Builder<BUILDER extends CollectionItemDefinition.Builder,
            ITEM extends CollectionItemDefinition<ELEMENT>, ELEMENT extends ItemDefinition>
            extends ItemDefinition.Builder<BUILDER, ITEM> {

        private final ELEMENT elementDefinition;

        Builder(String attributeName, ModelType type, ELEMENT elementDefinition) {
            super(attributeName, type);
            this.elementDefinition = elementDefinition;
        }

        Builder(ITEM basis) {
            this(null, basis);
        }

        Builder(String name, ITEM basis) {
            super(name, basis);
            this.elementDefinition = basis.getElementDefinition();
        }

        /**
         * Sets an overall validator for the list.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
        final BUILDER setCollectionValidator(ParameterValidator validator) {
            return super.setValidator(validator);
        }

        /**
         * Sets a maximum size for a collection-type item or one whose value is a string or byte[].
         * The value represents the maximum number of elements in the collection, or the maximum length of
         * the string or array. <strong>It does not represent a maximum value for a numeric item and should
         * not be configured for numeric items.</strong>
         * @param maxSize the maximum size
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setMaxSize(final int maxSize) {
            return super.setMax(maxSize);
        }

        /**
         * Sets a minimum size description for a collection-type item or one whose value is a string or byte[].
         * The value represents the minimum number of elements in the collection, or the minimum length of
         * the string or array. <strong>It does not represent a minimum value for a numeric item and should
         * not be configured for numeric items.</strong>
         * @param minSize the minimum size
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setMinSize(final int minSize) {
            return super.setMin(minSize);
        }

        private ELEMENT getElementDefinition() {
            return elementDefinition;
        }

    }
}
