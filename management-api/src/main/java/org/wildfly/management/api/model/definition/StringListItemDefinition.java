/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 * {@link ItemDefinition} for items whose values are lists with elements that are of {@link ModelType#STRING}.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public final class StringListItemDefinition extends ListItemDefinition<SimpleItemDefinition> {

    private StringListItemDefinition(Builder builder) {
        super(builder);
    }

    /** Builder for a {@link StringListItemDefinition}. */
    public static final class Builder extends ListItemDefinition.Builder<Builder, StringListItemDefinition, SimpleItemDefinition> {

        public static Builder of(final String name) {
            return new Builder(name);
        }

        public static Builder of(final StringListItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final StringListItemDefinition basis) {
            return new Builder(name, basis);
        }

        private Builder(final String name) {
            super(name, SimpleItemDefinition.Builder.of(ModelType.STRING).build());
            setAttributeParser(AttributeParser.STRING_LIST);
            setAttributeMarshaller(AttributeMarshaller.STRING_LIST);
        }

        private Builder(final String name, final StringListItemDefinition basis) {
            super(name, basis);
        }

        @Override
        public StringListItemDefinition build() {
            return new StringListItemDefinition(this);
        }

    }

}
