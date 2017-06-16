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

/**
 * {@link ItemDefinition} for fields that represent lists with
 * simple element types (i.e. not {@link ModelType#LIST} or {@link ModelType#OBJECT}.
 * The elements in the list are described by their own {@link ItemDefinition}, which allows a degree
 * of configurability that is not possible with the more straightforward {@link PrimitiveListItemDefinition}.
 *
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public final class SimpleListItemDefinition extends ListItemDefinition {
    private final SimpleItemDefinition valueType;

    private SimpleListItemDefinition(final ListItemDefinition.Builder builder, SimpleItemDefinition valueType) {
        super(builder);
        this.valueType = valueType;
        // This class is not appropriate for lists with complex elements. Use ObjectListAttributeDefinition
        assert valueType.getType() != ModelType.OBJECT && valueType.getType() != ModelType.LIST;
    }

    public ItemDefinition getValueType() {
        return valueType;
    }


    @Override
    public CapabilityReferenceRecorder getReferenceRecorder() {
        return valueType.getReferenceRecorder();
    }

    /** Builder for a {@link SimpleListItemDefinition}. */
    public static final class Builder extends ListItemDefinition.Builder<Builder,SimpleListItemDefinition>{

        public static Builder of(final String name, final SimpleItemDefinition valueType) {
            return new Builder(name, valueType);
        }

        public static Builder of(final SimpleListItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final SimpleListItemDefinition basis) {
            return new Builder(name, basis);
        }

        private final SimpleItemDefinition valueType;

        private Builder(final String name, final SimpleItemDefinition valueType) {
            super(name);
            this.valueType = valueType;
            setElementValidator(valueType.getValidator());
        }

        private Builder(final String name, final SimpleListItemDefinition basis) {
            super(name, basis);
            valueType = basis.valueType;
        }

        @Override
        public SimpleListItemDefinition build() {
            if (getAttributeMarshaller() == null) {
                setAttributeMarshaller(AttributeMarshallers.getSimpleListMarshaller(true));
            }
            //todo add parser for SimpleListAttributeDefinition, for now no one is using it yet.
            /*if (parser == null) {
                parser = AttributeParser..
            }*/
            return new SimpleListItemDefinition(this, valueType);
        }
    }
}
