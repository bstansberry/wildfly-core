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

import java.util.Objects;

import org.jboss.dmr.ModelType;

/**
 * {@link MapItemDefinition} for maps with both keys and values of {@link ModelType#STRING}.
 *
 * @author Jason T. Greene
 * @author Tomaz Cerar<
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 *
 * @deprecated Use {@link SimpleMapItemDefinition} and configure an {@link AttributeMarshaller} and {@link AttributeParser}
 */
@Deprecated
public final class PropertiesItemDefinition extends MapItemDefinition<SimpleItemDefinition> {

    private PropertiesItemDefinition(Builder builder) {
        super(builder);
    }

    public ModelType getValueType() {
        return ModelType.STRING;
    }

    @Override
    public Builder getBuilderToCopy() {
        return Builder.of(this);
    }

    /**
     * Builder for a {@link PropertiesItemDefinition}.
     * @deprecated Use {@link SimpleMapItemDefinition.Builder} and configure an {@link AttributeMarshaller} and {@link AttributeParser}
     */
    @Deprecated
    public static final class Builder extends MapItemDefinition.Builder<Builder, PropertiesItemDefinition, SimpleItemDefinition> {

        public static Builder of(final String name) {
            return new Builder(name);
        }

        public static Builder of(final PropertiesItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final PropertiesItemDefinition basis) {
            return new Builder(name, basis);
        }

        private boolean wrapXmlElement = true;
        private String wrapperElement = null;
        private boolean xmlNameExplicitlySet = false;

        private Builder(final String name) {
            super(name, SimpleItemDefinition.Builder.of(ModelType.STRING).build());
        }

        private Builder(final String name, final PropertiesItemDefinition basis) {
            super(name, basis);
        }

        /**
         * Sets whether the xml representation of this item should include a wrapper element around
         * the elements that represent the entries in the map. Default is {@code true}.
         * @param wrap {@code true} if a wrapper element should be marshalled.
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public Builder setWrapXmlElement(boolean wrap) {
            this.wrapXmlElement = wrap;
            return this;
        }

        /**
         * If using an xml wrapper element {@link #setWrapXmlElement(boolean) is enabled}, sets the
         * name that should be used for the wrapper element. If not set the name of the item will
         * be used.
         * @param name the name that should be used for the wrapper element
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings("unused")
        public Builder setWrapperElement(String name) {
            this.wrapperElement = name;
            return this;
        }

        @Override
        public Builder setXmlName(String xmlName) {
            this.xmlNameExplicitlySet = true;
            return super.setXmlName(xmlName);
        }

        @SuppressWarnings("deprecation")
        @Override
        @Deprecated
        public Builder setAllowExpression(boolean allowExpression) {
            return super.setAllowExpression(allowExpression);
        }

        @Override
        public PropertiesItemDefinition build() {

            String xmlName = this.getXmlName();
            String elementName = Objects.equals(this.getName(), xmlName) ? null : xmlName;
            if (getAttributeMarshaller() == null) {
                setAttributeMarshaller(new AttributeMarshallers.PropertiesAttributeMarshaller(this.wrapperElement, this.xmlNameExplicitlySet ? xmlName : elementName, this.wrapXmlElement));
            }

            if (getParser() == null) {
                setAttributeParser(new AttributeParsers.PropertiesParser(this.wrapperElement, elementName, this.wrapXmlElement));
            }

            return new PropertiesItemDefinition(this);
        }
    }
}
