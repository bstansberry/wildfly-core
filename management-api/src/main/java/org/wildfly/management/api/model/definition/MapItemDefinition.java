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
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Defining characteristics of an {@link ItemDefinition item} whose {@link ItemDefinition#getType() type}
 * is {@link ModelType#OBJECT}, where the set of map keys is not fixed and where all children of the object
 * have values of the same type; i.e. the item represents a logical map of arbitrary key value pairs
 * analogous to a java {@code Map<String, ?>}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class MapItemDefinition<VALUE extends ItemDefinition> extends CollectionItemDefinition<VALUE> {

    MapItemDefinition(Builder<? extends Builder, ? extends MapItemDefinition, VALUE> builder) {
        super(builder);
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



    /** Builder for creating a {@link MapItemDefinition} */
    public abstract static class Builder<BUILDER extends Builder, ITEM extends MapItemDefinition<VALUE>, VALUE extends ItemDefinition>
            extends CollectionItemDefinition.Builder<BUILDER, ITEM, VALUE> {

        Builder(final String name, final VALUE valueDefinition) {
            super(name, ModelType.OBJECT, valueDefinition);
        }

        Builder(final MapItemDefinition<VALUE> basis) {
            this(null, basis);
        }

        Builder(final String name, final MapItemDefinition<VALUE> basis) {
            super(name, basis);
        }

        /**
         * Sets an overall validator for the map. This is only relevant to operation parameter
         * use cases. The item definition produced by this builder will directly enforce the item's
         * {@link ItemDefinition#isRequired()} () required} and
         * {@link ItemDefinition#isAllowExpression() allow expression} settings, so the given {@code validator}
         * need not concern itself with those validations.
         * <p>
         * <strong>Usage note:</strong> Providing a validator should be limited to atypical custom validation cases.
         * Standard validation against the item's definition (i.e. checking for correct type, size within
         * min and max and validity of each element) will be automatically handled without
         * requiring provision of a validator.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the field definition
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public BUILDER setMapValidator(ParameterValidator validator) {
            return super.setCollectionValidator(validator);
        }

    }
}
