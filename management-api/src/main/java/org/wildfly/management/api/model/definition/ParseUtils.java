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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.dmr.ModelType.PROPERTY;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.projectodd.vdx.core.ErrorType;
import org.projectodd.vdx.core.ValidationError;
import org.projectodd.vdx.core.XMLStreamValidationException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ParseUtils {

    private ParseUtils() {
    }

//    public static Element nextElement(XMLExtendedStreamReader reader) throws XMLStreamException {
//        if (reader.nextTag() == END_ELEMENT) {
//            return null;
//        }
//
//        return Element.forName(reader.getLocalName());
//    }
//
//    /**
//     * A variation of nextElement that verifies the nextElement is not in a different namespace.
//     *
//     * @param reader the XmlExtendedReader to read from.
//     * @param expectedNamespace the namespace expected.
//     * @return the element or null if the end is reached
//     * @throws XMLStreamException if the namespace is wrong or there is a problem accessing the reader
//     */
//    public static Element nextElement(XMLExtendedStreamReader reader, Namespace expectedNamespace) throws XMLStreamException {
//        Element element = nextElement(reader);
//
//        if (element == null) {
//            return element;
//        } else if (element != Element.UNKNOWN
//                && expectedNamespace.equals(Namespace.forUri(reader.getNamespaceURI()))) {
//            return element;
//        }
//
//        throw unexpectedElement(reader);
//    }

    /**
     * Get an exception reporting an unexpected XML element.
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.unexpectedElement(reader.getName(), reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.UNEXPECTED_ELEMENT)
                                                        .element(reader.getName()),
                                                ex);
    }

    /**
     * Get an exception reporting an unexpected XML element.
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader, Set<String> possible) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.unexpectedElement(reader.getName(), asStringList(possible), reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.UNEXPECTED_ELEMENT)
                                                        .element(reader.getName())
                                                        .alternatives(possible),
                                                ex);
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.unexpectedAttribute(reader.getAttributeName(index), reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.UNEXPECTED_ATTRIBUTE)
                                                        .element(reader.getName())
                                                        .attribute(reader.getAttributeName(index)),
                                                ex);
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     * @param reader the stream reader
     * @param index the attribute index
     * @param possibleAttributes attributes that are expected on this element
     * @return the exception
     */
    public static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index, Set<String> possibleAttributes) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.unexpectedAttribute(reader.getAttributeName(index), asStringList(possibleAttributes), reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.UNEXPECTED_ATTRIBUTE)
                                                        .element(reader.getName())
                                                        .attribute(reader.getAttributeName(index))
                                                        .alternatives(possibleAttributes),
                                                ex);
    }

    private static StringBuilder asStringList(Set<?> attributes) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return b;
    }
    /**
     * Get an exception reporting an invalid XML attribute value.
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException invalidAttributeValue(final XMLExtendedStreamReader reader, final int index) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.invalidAttributeValue(reader.getAttributeValue(index), reader.getAttributeName(index), reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.INVALID_ATTRIBUTE_VALUE)
                                                        .element(reader.getName())
                                                        .attribute(reader.getAttributeName(index))
                                                        .attributeValue(reader.getAttributeValue(index)),
                                                ex);
    }

    private static XMLStreamException wrapMissingRequiredAttribute(final XMLStreamException ex, final XMLStreamReader reader, final Set<String> required) {
        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.REQUIRED_ATTRIBUTE_MISSING)
                                                        .element(reader.getName())
                                                        .alternatives(required),
                                                ex);
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
        return wrapMissingRequiredAttribute(ControllerLoggerDuplicate.ROOT_LOGGER.missingRequiredAttributes(asStringList(required),
                                                                                                   reader.getLocation()),
                                            reader,
                                            required.stream().map(Object::toString).collect(Collectors.toSet()));
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final String... required) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < required.length; i++) {
            final String o = required[i];
            b.append(o);
            if (required.length > i + 1) {
                b.append(", ");
            }
        }

        return wrapMissingRequiredAttribute(ControllerLoggerDuplicate.ROOT_LOGGER.missingRequiredAttributes(b, reader.getLocation()),
                                            reader, new HashSet<>(Arrays.asList(required)));
    }

    /**
     * Get an exception reporting a missing, required XML child element.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequiredElement(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.missingRequiredElements(b, reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.REQUIRED_ELEMENTS_MISSING)
                                                        .element(reader.getName())
                                                        .alternatives(required.stream().map(Object::toString)
                                                                              .collect(Collectors.toSet())),
                                                ex);
    }

    /**
     * Checks that the current element has no attributes, throwing an
     * {@link XMLStreamException} if one is found.
     * @param reader the reader
     * @throws XMLStreamException if an error occurs
     */
    public static void requireNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
    }

    /**
     * Consumes the remainder of the current element, throwing an
     * {@link XMLStreamException} if it contains any child
     * elements.
     * @param reader the reader
     * @throws XMLStreamException if an error occurs
     */
    public static void requireNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Get an exception reporting that an element of a given type and name has
     * already been declared in this scope.
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    public static XMLStreamException duplicateNamedElement(final XMLExtendedStreamReader reader, final String name) {
        final XMLStreamException ex = ControllerLoggerDuplicate.ROOT_LOGGER.duplicateNamedElement(name, reader.getLocation());

        return new XMLStreamValidationException(ex.getMessage(),
                                                ValidationError.from(ex, ErrorType.DUPLICATE_ELEMENT)
                                                        .element(reader.getName())
                                                        .attribute(QName.valueOf("name"))
                                                        .attributeValue(name),
                                                ex);
    }

    /**
     * Read an element which contains only a single string attribute.
     * @param reader the reader
     * @param attributeName the attribute name, usually "value" or "name"
     * @return the string value
     * @throws XMLStreamException if an error occurs or if the
     *         element does not contain the specified attribute, contains other
     *         attributes, or contains child elements.
     */
    public static String readStringAttributeElement(final XMLExtendedStreamReader reader, final String attributeName)
            throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final String value = reader.getAttributeValue(0);
        requireNoContent(reader);
        return value;
    }

    /**
     * Require that the current element have only a single attribute with the
     * given name.
     * @param reader the reader
     * @param attributeName the attribute name
     * @throws XMLStreamException if an error occurs
     */
    public static void requireSingleAttribute(final XMLExtendedStreamReader reader, final String attributeName)
            throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count == 0) {
            throw missingRequired(reader, Collections.singleton(attributeName));
        }
        requireNoNamespaceAttribute(reader, 0);
        if (!attributeName.equals(reader.getAttributeLocalName(0))) {
            throw unexpectedAttribute(reader, 0);
        }
        if (count > 1) {
            throw unexpectedAttribute(reader, 1);
        }
    }

    /**
     * Require all the named attributes, returning their values in order.
     * @param reader the reader
     * @param attributeNames the attribute names
     * @return the attribute values in order
     * @throws XMLStreamException if an error occurs
     */
    public static String[] requireAttributes(final XMLExtendedStreamReader reader, final String... attributeNames)
            throws XMLStreamException {
        final int length = attributeNames.length;
        final String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            final String name = attributeNames[i];
            final String value = reader.getAttributeValue(null, name);
            if (value == null) {
                throw missingRequired(reader, Collections.singleton(name));
            }
            result[i] = value;
        }
        return result;
    }

    public static boolean isNoNamespaceAttribute(final XMLExtendedStreamReader reader, final int index) {
        String namespace = reader.getAttributeNamespace(index);
        // FIXME when STXM-8 is done, remove the null check
        return namespace == null || XMLConstants.NULL_NS_URI.equals(namespace);
    }

    public static void requireNoNamespaceAttribute(final XMLExtendedStreamReader reader, final int index)
            throws XMLStreamException {
        if (!isNoNamespaceAttribute(reader, index)) {
            throw unexpectedAttribute(reader, index);
        }
    }

    public static ModelNode parseAttributeValue(final String value, final boolean isExpressionAllowed, final ModelType attributeType) {
        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null) {
            if (isExpressionAllowed && isExpression(trimmed)) {
                node = new ModelNode(new ValueExpression(trimmed));
            } else {
                if(attributeType == STRING || attributeType == PROPERTY) {
                    node = new ModelNode().set(value);
                } else {
                    node = new ModelNode().set(trimmed);
                }
            }
            if (node.getType() != ModelType.EXPRESSION) {
                // Convert the string to the expected type
                // This is a convenience only and is not a requirement
                // of this method
                try {
                    switch (attributeType) {
                        case BIG_DECIMAL:
                            node.set(node.asBigDecimal());
                            break;
                        case BIG_INTEGER:
                            node.set(node.asBigInteger());
                            break;
                        case BOOLEAN:
                            node.set(node.asBoolean());
                            break;
                        case BYTES:
                            node.set(node.asBytes());
                            break;
                        case DOUBLE:
                            node.set(node.asDouble());
                            break;
                        case INT:
                            node.set(node.asInt());
                            break;
                        case LONG:
                            node.set(node.asLong());
                            break;
                    }
                } catch (IllegalArgumentException iae) {
                    // ignore and return the unconverted node
                }
            }
        } else {
            node = new ModelNode();
        }
        return node;
    }

    public static boolean isExpression(String value) {
        int openIdx = value.indexOf("${");
        return openIdx > -1 && value.lastIndexOf('}') > openIdx;
    }

    public static ModelNode parsePossibleExpression(String value) {
        ModelNode result = new ModelNode();
        if (isExpression(value)) {
            result.set(new ValueExpression(value));
        }
        else {
            result.set(value);
        }
        return result;
    }
}
