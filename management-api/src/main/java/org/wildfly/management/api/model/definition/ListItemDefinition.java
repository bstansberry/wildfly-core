/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.management.api.model.definition;

import org.jboss.dmr.ModelType;
import org.wildfly.common.Assert;
import org.wildfly.management.api.model.validation.ListValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Defining characteristics of an {@link ItemDefinition item} whose {@link ItemDefinition#getType() type}
 * is {@link ModelType#LIST}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
abstract class ListItemDefinition extends ItemDefinition {

    private final ParameterValidator elementValidator;

    ListItemDefinition(ListItemDefinition.Builder<?, ?> builder) {
        super(builder);
        this.elementValidator = builder.getElementValidator();
    }

    /**
     * The validator used to validate elements in the list.
     * @return the element validator
     */
    ParameterValidator getElementValidator() {
        return elementValidator;
    }

    /** Builder for creating a {@link ListItemDefinition} */
    abstract static class Builder<BUILDER extends Builder, ITEM extends ListItemDefinition>
            extends ItemDefinition.Builder<BUILDER, ITEM> {

        private ParameterValidator elementValidator;
        private boolean allowNullElement;
        private boolean allowDuplicates = true;

        Builder(String attributeName) {
            super(attributeName, ModelType.LIST);
            this.setAttributeParser(AttributeParser.STRING_LIST);
        }

        Builder(ListItemDefinition basis) {
            this(null, basis);
        }

        Builder(String name, ListItemDefinition basis) {
            super(name, basis);
            this.elementValidator = basis.getElementValidator();
            this.setAttributeParser(AttributeParser.STRING_LIST);
        }

        /**
         * Gets the validator to use for validating list elements.
         * @return the validator, or {@code null} if no validator has been set
         */
        ParameterValidator getElementValidator() {
            if (elementValidator == null) {
                return null;
            }

            ParameterValidator toWrap = elementValidator;
            ParameterValidator wrappedElementValidator = null;
            if (elementValidator instanceof  NillableOrExpressionParameterValidator) {
                // See if it's configured correctly already; if so don't re-wrap
                NillableOrExpressionParameterValidator wrapped = (NillableOrExpressionParameterValidator) elementValidator;
                Boolean allow = wrapped.getAllowNull();
                if ((allow == null || allow) == allowNullElement
                        && wrapped.isAllowExpression() == isAllowExpression()) {
                    wrappedElementValidator = wrapped;
                } else {
                    // re-wrap
                    toWrap = wrapped.getDelegate();
                }
            }
            if (wrappedElementValidator == null) {
                elementValidator = new NillableOrExpressionParameterValidator(toWrap, allowNullElement, isAllowExpression());
            }
            return elementValidator;
        }

        /**
         * Sets the validator to use for validating list elements.
         *
         * @param elementValidator the validator
         * @return a builder that can be used to continue building the item definition
         *
         * @throws IllegalArgumentException if {@code elementValidator} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public final BUILDER setElementValidator(ParameterValidator elementValidator) {
            Assert.checkNotNullParam("elementValidator", elementValidator);
            this.elementValidator = elementValidator;
            // Setting an element validator invalidates any existing overall attribute validator
            setListValidator(null);
            return (BUILDER) this;
        }

        /**
         * Sets an overall validator for the list.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings("WeakerAccess")
        public BUILDER setListValidator(ParameterValidator validator) {
            return super.setValidator(validator);
        }

        /**
         * Overrides the superclass to throw {@code UnsupportedOperationException}. Use
         * {@link #setElementValidator(ParameterValidator)} to configure element validation or
         * use {@link #setListValidator(ParameterValidator)} to
         * set an overall validator for the list.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public BUILDER setValidator(ParameterValidator validator) {
            // TODO remove this from the superclass and have a "simple" variant?
            throw new UnsupportedOperationException();
        }

        /**
         * Sets whether undefined list elements are valid.
         * @param allowNullElement whether undefined elements are valid
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"unchecked", "unused"})
        public BUILDER setAllowNullElement(boolean allowNullElement) {
            this.allowNullElement = allowNullElement;
            return (BUILDER) this;
        }

        /**
         * toggles default validator strategy to allow / not allow duplicate elements in list
         * @param allowDuplicates false if duplicates are not allowed
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"unchecked", "unused"})
        public BUILDER setAllowDuplicates(boolean allowDuplicates) {
            this.allowDuplicates = allowDuplicates;
            return (BUILDER) this;
        }

        @Override
        ParameterValidator getValidator() {
            ParameterValidator result = super.getValidator();
            if (result == null) {
                ParameterValidator listElementValidator = getElementValidator();
                // Subclasses must call setElementValidator before calling this
                assert listElementValidator != null;
                result = new ListValidator(getElementValidator(), getMinSize(), getMaxSize(), allowDuplicates);
            }
            return result;
        }

        @Override
        Integer getMinSize() {
            int minSize = super.getMinSize();
            if (minSize < 0) {
                setMinSize(0);
            }
            return minSize;
        }

        @Override
        Integer getMaxSize() {
            int maxSize = super.getMaxSize();
            if (maxSize < 1) {
                setMaxSize(Integer.MAX_VALUE);
            }
            return maxSize;
        }
    }
}
