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

/**
 * {@link ItemDefinition} for items that are lists of {@link ObjectTypeItemDefinition object items}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ObjectListItemDefinition extends ListItemDefinition<ObjectTypeItemDefinition> {

    private ObjectListItemDefinition(Builder builder) {
        super(builder);
    }

    /** Builder for an {@link org.wildfly.management.api.model.definition.ObjectListItemDefinition}. */
    public static final class Builder extends ListItemDefinition.Builder<Builder, ObjectListItemDefinition, ObjectTypeItemDefinition> {

        public static Builder of(final String name, final ObjectTypeItemDefinition valueType) {
            return new Builder(name, valueType);
        }

        public static Builder of(final ObjectListItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final ObjectListItemDefinition basis) {
            return new Builder(name, basis);
        }

        private Builder(final String name, final ObjectTypeItemDefinition valueType) {
            super(name, valueType);
            setAttributeParser(AttributeParser.OBJECT_LIST_PARSER);
            setAttributeMarshaller(AttributeMarshaller.OBJECT_LIST_MARSHALLER);
        }

        private Builder(final String name, final ObjectListItemDefinition basis) {
            super(name, basis);
        }

        public ObjectListItemDefinition build() {
            return new ObjectListItemDefinition(this);
        }

    }
}
