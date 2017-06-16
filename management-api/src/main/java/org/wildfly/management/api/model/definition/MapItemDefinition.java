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


import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.common.Assert;
import org.wildfly.management.api.model.validation.MapValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Defining characteristics of an {@link ItemDefinition item} whose {@link ItemDefinition#getType() type}
 * is {@link ModelType#OBJECT}, where the set of map keys is not fixed and where all children of the object
 * have values of the same type; i.e. the item represents a logical map of arbitrary key value pairs
 * analogous to a java {@code Map<String, ?>}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class MapItemDefinition extends ItemDefinition {

    private final ParameterValidator elementValidator;

    MapItemDefinition(Builder<? extends Builder, ? extends MapItemDefinition> builder) {
        super(builder);
        this.elementValidator = builder.getElementValidator();
    }

    /**
     * A {@link ParameterCorrector} that can correct a parameter of {@link ModelType#LIST} and
     * convert it to {@link ModelType#OBJECT}. A call to {@code newValue.asPropertyList()} must
     * succeed for any correction to be done.
     */
    @SuppressWarnings("unused")
    public static final ParameterCorrector LIST_TO_MAP_CORRECTOR = new ParameterCorrector() {
        public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
            if (newValue.isDefined()) {
                if (newValue.getType() == ModelType.LIST) {
                    List<Property> propertyList;
                    try {
                        propertyList = newValue.asPropertyList();
                    } catch (RuntimeException e) {
                        // can't correct
                        return newValue;
                    }

                    ModelNode corrected = new ModelNode().setEmptyObject();
                    for (Property p : propertyList) {
                        corrected.get(p.getName()).set(p.getValue());
                    }
                    return corrected;
                }
            }
            return newValue;
        }
    };

    /**
     * The validator used to validate elements in the list.
     * @return  the element validator
     */
    ParameterValidator getElementValidator() {
        return elementValidator;
    }


    /** Builder for creating a {@link MapItemDefinition} */
    public abstract static class Builder<BUILDER extends Builder, ITEM extends MapItemDefinition>
            extends ItemDefinition.Builder<BUILDER, ITEM> {

        ParameterValidator elementValidator;
        private boolean allowNullElement;

        Builder(final String name) {
            super(name, ModelType.OBJECT);
        }

        Builder(final MapItemDefinition basis) {
            this(null, basis);
        }

        Builder(final String name, final MapItemDefinition basis) {
            super(name, basis);
            this.elementValidator = basis.elementValidator;
            if (elementValidator instanceof NillableOrExpressionParameterValidator) {
                this.allowNullElement = ((NillableOrExpressionParameterValidator) elementValidator).getAllowNull();
            }
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
            if (elementValidator instanceof NillableOrExpressionParameterValidator) {
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
         * Sets the validator to use for validating the value of map entries.
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
            setMapValidator(null);
            return (BUILDER) this;
        }



        /**
         * Sets whether undefined list elements are valid.
         * @param allowNullElement whether undefined elements are valid
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings({"unchecked", "unused"})
        public BUILDER setAllowNullElement(boolean allowNullElement) {
            this.allowNullElement = allowNullElement;
            return (BUILDER) this;
        }

        /**
         * Sets an overall validator for the map.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the field definition
         */
        @SuppressWarnings("WeakerAccess")
        public BUILDER setMapValidator(ParameterValidator validator) {
            return super.setValidator(validator);
        }

        /**
         * Overrides the superclass to throw {@code UnsupportedOperationException}. Use
         * {@link #setElementValidator(ParameterValidator)} to configure element validation or
         * use {@link #setMapValidator(ParameterValidator)} (ParameterValidator)} to
         * set an overall validator for the list.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public BUILDER setValidator(ParameterValidator validator) {
            // TODO remove this from the superclass and have a "simple" variant?
            throw new UnsupportedOperationException();
        }

        @Override
        Integer getMinSize() {
            Integer minSize = super.getMinSize();
            if (minSize != null && minSize < 0) {
                setMinSize(0);
            }
            return minSize;
        }

        @Override
        Integer getMaxSize() {
            Integer maxSize = super.getMaxSize();
            if (maxSize != null && maxSize < 1) {
                setMaxSize(Integer.MAX_VALUE);
            }
            return maxSize;
        }

        @Override
        ParameterValidator getValidator() {
            ParameterValidator result = super.getValidator();
            if (result == null) {
                ParameterValidator mapElementValidator = getElementValidator();
                // Subclasses must call setElementValidator before calling this
                assert mapElementValidator != null;
                result = new MapValidator(getElementValidator(), getMinSize(), getMaxSize());
            }
            return result;
        }
    }
}
