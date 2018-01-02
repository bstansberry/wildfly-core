/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.management.api.model.definition;


import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.management.api.OperationClientException;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public abstract class AttributeParser {

    /**
     * Creates a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link ItemDefinition#getValidator() this object's validator}., and then stores it in the given {@code operation}
     * model node as a key/value pair whose key is this attribute's getName() name}.
     * <p>
     * If {@code value} is {@code null} an {@link org.jboss.dmr.ModelType#UNDEFINED undefined} node will be stored if such a value
     * is acceptable to the validator.
     * </p>
     * <p>
     * The expected usage of this method is in parsers seeking to build up an operation to store their parsed data
     * into the configuration.
     * </p>
     *
     * @param value     the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param operation model node of type {@link org.jboss.dmr.ModelType#OBJECT} into which the parsed value should be stored
     * @param reader    {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *                  the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *                  the given value is invalid.
     * @throws XMLStreamException if {@code value} is not valid
     */
    public void parseAndSetParameter(final ItemDefinition attribute, final String value, final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException {
        ModelNode paramVal = parse(attribute, value, reader);
        operation.get(attribute.getName()).set(paramVal);
    }

    /**
     * Creates and returns a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link ItemDefinition#getValidator() this object's validator}.
     * <p>
     * If {@code value} is {@code null} an {@link org.jboss.dmr.ModelType#UNDEFINED undefined} node will be returned.
     * </p>
     *
     * @param value  the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param reader {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *               the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *               the given value is invalid.
     * @return {@code ModelNode} representing the parsed value
     * @throws XMLStreamException if {@code value} is not valid
     */
    public ModelNode parse(final ItemDefinition attribute, final String value, final XMLStreamReader reader) throws XMLStreamException {
        try {
            return parse(attribute, value);
        } catch (OperationClientException e) {
            throw new XMLStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
    }

    private ModelNode parse(final ItemDefinition attribute, final String value) throws OperationClientException {
        ModelNode node = ParseUtils.parseAttributeValue(value, attribute.isAllowExpression(), attribute.getType());
        ItemDefinition validating;
        // A bit yuck, but I didn't want to introduce a new type just for this
        if (attribute instanceof CollectionItemDefinition) {
            validating = ((CollectionItemDefinition) attribute).getElementDefinition();
        } else {
            validating = attribute;
        }
        ItemDefinitionValidator.validateItem(validating, node);

        return node;
    }

    public boolean isParseAsElement() {
        return false;
    }

    public void parseElement(final ItemDefinition attribute, final XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

    }

    public String getXmlName(final ItemDefinition attribute){
        return attribute.getXmlName();
    }

    public static final AttributeParser SIMPLE = new AttributeParser() {
    };

    public static final AttributeParser STRING_LIST = new AttributeParser() {
        @Override
        public void parseAndSetParameter(ItemDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            if (value == null) return;
            final ModelNode node = operation.get(attribute.getName()).setEmptyList();
            if (!value.isEmpty()) {
                for (final String element : value.split("\\s+")) {
                    node.add(parse(attribute, element, reader));
                }
            }
        }
    };

    public static final AttributeParser COMMA_DELIMITED_STRING_LIST = new AttributeParser() {
        @Override
        public void parseAndSetParameter(ItemDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
            if (value == null) return;
            final ModelNode node = operation.get(attribute.getName()).setEmptyList();
            if (!value.isEmpty()) {
                for (String element : value.split(",")) {
                    node.add(parse(attribute, element, reader));
                }
            }
        }
    };

    public static final class DiscardOldDefaultValueParser extends AttributeParser{
        private final String value;

        public DiscardOldDefaultValueParser(String value) {
            this.value = value;
        }

        @Override
        public ModelNode parse(ItemDefinition attribute, String value, XMLStreamReader reader) throws XMLStreamException {
            if (!this.value.equals(value)) { //if default value set, ignore it!
                return super.parse(attribute, value, reader);
            }
            return new ModelNode();
        }
    }

    public static final AttributeParser PROPERTIES_PARSER = new AttributeParsers.PropertiesParser();

    public static final AttributeParser PROPERTIES_PARSER_UNWRAPPED = new AttributeParsers.PropertiesParser(false);

    public static final AttributeParser OBJECT_PARSER = new AttributeParsers.ObjectParser();

    public static final AttributeParser OBJECT_LIST_PARSER = AttributeParsers.WRAPPED_OBJECT_LIST_PARSER;

    public static final AttributeParser WRAPPED_OBJECT_LIST_PARSER = AttributeParsers.WRAPPED_OBJECT_LIST_PARSER;
    public static final AttributeParser UNWRAPPED_OBJECT_LIST_PARSER = AttributeParsers.UNWRAPPED_OBJECT_LIST_PARSER;

}
