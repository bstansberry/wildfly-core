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

package org.jboss.as.controller;

import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.management.api.model.definition.ItemDefinition;
import org.wildfly.management.api.model.definition.PropertiesItemDefinition;

/**
 * Represents simple key=value map equivalent of java.util.Map<String,String>()
 *
 * @author Jason T. Greene
 * @author Tomaz Cerar<
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
//todo maybe replace with SimpleMapAttributeDefinition?
public final class PropertiesAttributeDefinition extends MapAttributeDefinition {

    private final boolean wrapXmlElement;
    private final String wrapperElement;

    private PropertiesAttributeDefinition(final Builder builder) {
        super(builder);
        this.wrapXmlElement = builder.wrapXmlElement;
        this.wrapperElement = builder.wrapperElement;
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    void setValueType(ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        if (isAllowExpression()) {
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(true));
        }
    }

    public Map<String, String> unwrap(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        ModelNode value = resolveModelAttribute(context, model);
        if (value.isDefined()) {
            return unwrapModel(context, value);
        } else {
            return Collections.emptyMap();
        }
    }

    public static Map<String, String> unwrapModel(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        if (!model.isDefined()) return Collections.emptyMap();
        Map<String, String> props = new HashMap<>();
        for (Property p : model.asPropertyList()) {
            // TODO this is wasteful if we are called from unwrap(...) as the passed in model is already fully resolved
            ModelNode value = context.resolveExpressions(p.getValue());
            props.put(p.getName(), value.isDefined() ? value.asString() : null);
        }
        return props;
    }
    @Deprecated
    public void parse(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
        parseAndAddParameterElement(array[0], array[1], operation, reader);
        ParseUtils.requireNoContent(reader);
    }

    public boolean isWrapped() {
        return wrapXmlElement;
    }

    @SuppressWarnings("unused")
    public String getWrapperElement() {
        return wrapperElement;
    }

    public static class Builder extends MapAttributeDefinition.Builder<Builder, PropertiesAttributeDefinition> {
        private boolean wrapXmlElement = true;
        private String wrapperElement = null;
        //for backward compatibility, until we get new core out and used by wildfly full.
        private boolean xmlNameExplicitlySet = false;

        public Builder(final String name, boolean optional) {
            super(name, optional);
        }

        public Builder(final PropertiesAttributeDefinition basis) {
            super(basis);
        }

        public Builder(final MapAttributeDefinition basis) {
            super(basis);
        }

        /**
         *
         * @deprecated use setParser(new AttributeParser.PropertiesParsers(wrapper)
         */
        @Deprecated
        public Builder setWrapXmlElement(boolean wrap) {
            this.wrapXmlElement = wrap;
            return this;
        }

        /**
         * @deprecated use setParser(new AttributeParser.PropertiesParsers(wrapper)
         */
        @Deprecated
        public Builder setWrapperElement(String name) {
            this.wrapperElement = name;
            return this;
        }

        /**
         * @deprecated use setParser(new AttributeParser.PropertiesParsers(wrapper)
         */
        @Override
        public Builder setXmlName(String xmlName) {
            this.xmlNameExplicitlySet = true;
            return super.setXmlName(xmlName);
        }

        @Override
        public PropertiesAttributeDefinition build() {
            // Set up the legacy business where by default a nillable collection means undefined elements are allowed
            configureUndefinedElement();

            String xmlName = getXmlName();
            String elementName = getName().equals(xmlName) ? null : xmlName;
            if (getAttributeMarshaller() == null) {
                setAttributeMarshaller(new AttributeMarshallers.PropertiesAttributeMarshaller(wrapperElement, xmlNameExplicitlySet ? xmlName : elementName, wrapXmlElement));
            }
            if (getParser() == null) {
                setAttributeParser(new AttributeParsers.PropertiesParser(wrapperElement, elementName, wrapXmlElement));
            }

            return new PropertiesAttributeDefinition(this);
        }

        @Override
        protected ItemDefinition.Builder createItemDefinitionBuilder() {
            return PropertiesItemDefinition.Builder.of(getName());
        }
    }
}
