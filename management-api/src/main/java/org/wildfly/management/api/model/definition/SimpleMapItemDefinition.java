/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2012, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.wildfly.management.api.model.definition;

import org.jboss.dmr.ModelType;
import org.wildfly.management.api.model.validation.ModelTypeValidator;

/**
 * {@link MapItemDefinition} for maps with keys of {@link ModelType#STRING} and
 * values of a simple ModelType.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @since 7.2
 */
@SuppressWarnings("WeakerAccess")
public final class SimpleMapItemDefinition extends MapItemDefinition {

    private final ModelType valueType;

    SimpleMapItemDefinition(final Builder builder) {
        super(builder);
        this.valueType = builder.valueType;
    }

    public ModelType getValueType() {
        return valueType;
    }

    /** Builder for a {@link org.wildfly.management.api.model.definition.SimpleMapItemDefinition */
    public static class Builder  extends MapItemDefinition.Builder<Builder, SimpleMapItemDefinition> {

        public static Builder of(final String name, final ModelType valueType) {
            return new Builder(name, valueType, true);
        }

        public static Builder of(final SimpleMapItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final SimpleMapItemDefinition basis) {
            return new Builder(name, basis);
        }

        private final ModelType valueType;

        Builder(final String name, final ModelType valueType, boolean setDefaultParserMarshaller) {
            super(name);
            this.valueType = valueType;
            if (setDefaultParserMarshaller) {
                this.setAttributeParser(AttributeParser.PROPERTIES_PARSER);
                this.setAttributeMarshaller(AttributeMarshaller.PROPERTIES_MARSHALLER);
            }
        }

        Builder(final String name, final SimpleMapItemDefinition basis) {
            super(name, basis);
            this.valueType = basis.valueType;
        }

        @Override
        public SimpleMapItemDefinition build() {
            if (elementValidator == null) {
                assert valueType != null;
                elementValidator = new ModelTypeValidator(valueType);
            }
            return new SimpleMapItemDefinition(this);
        }
    }
}
