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

import java.util.ArrayList;
import java.util.Arrays;

import org.jboss.dmr.ModelType;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * {@link ItemDefinition} for items of type {@link ModelType#OBJECT} that aren't maps, but
 * rather a set fixed keys where each key is associated with a value that is itself a
 * {@link ItemDefinition defined item}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 *
 * @see MapItemDefinition
 */
public final class ObjectTypeItemDefinition extends ItemDefinition {
    private final ItemDefinition[] valueTypes;
    private final String suffix;

    private ObjectTypeItemDefinition(Builder builder) {
        super(builder);
        this.valueTypes = builder.getValueTypes();
        String sfx = builder.getSuffix();
        this.suffix = sfx == null ? "" : sfx;
    }

    public ItemDefinition[] getValueTypes() {
        return valueTypes;
    }

    public String getSuffix() {
        return suffix;
    }

    /** Builder for an {@link ObjectTypeItemDefinition}*/
    public static final class Builder extends ItemDefinition.Builder<Builder, ObjectTypeItemDefinition> {

        public static Builder of(final String name, final ItemDefinition... valueTypes) {
            return new Builder(name, valueTypes);
        }

        public static Builder of(final String name, final ItemDefinition[] valueTypes, final ItemDefinition[] moreValueTypes) {
            ArrayList<ItemDefinition> list = new ArrayList<>(Arrays.asList(valueTypes));
            list.addAll(Arrays.asList(moreValueTypes));
            ItemDefinition[] allValueTypes = new ItemDefinition[list.size()];
            list.toArray(allValueTypes);

            return new Builder(name, allValueTypes);
        }

        public static Builder of(final ObjectTypeItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final ObjectTypeItemDefinition basis) {
            return new Builder(name, basis);
        }

        private String suffix;
        private final ItemDefinition[] valueTypes;

        private Builder(final String name, final ItemDefinition... valueTypes) {
            super(name, ModelType.OBJECT);
            this.valueTypes = valueTypes;
            setAttributeParser(AttributeParser.OBJECT_PARSER);
            setAttributeMarshaller(AttributeMarshaller.ATTRIBUTE_OBJECT);
        }

        private Builder(final String name, final ObjectTypeItemDefinition basis) {
            super(name, basis);
            this.valueTypes = basis.getValueTypes();
            this.suffix = basis.getSuffix();
        }

        /**
         * Sets a suffix to append to keys when looking up entries related to this item from a
         * {@link ResourceDescriptionResolver}.
         *
         * @param suffix the suffix
         * @return a builder that can be used to continue building the item definition
         *
         * @throws IllegalArgumentException if {@code elementValidator} is {@code null}
         */
        public Builder setSuffix(final String suffix) {
            this.suffix = suffix;
            return this;
        }

        @Override
        public Builder setValidator(ParameterValidator validator) {
            return super.setValidator(validator);
        }

        @Override
        public ObjectTypeItemDefinition build() {
            return new ObjectTypeItemDefinition(this);
        }

        private ItemDefinition[] getValueTypes() {
            return valueTypes;
        }

        private String getSuffix() {
            return suffix;
        }
    }

}
