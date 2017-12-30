/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.wildfly.management.api._private;

import static org.jboss.logging.annotations.Message.NONE;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.OperationFailedException;

/**
 * Duplicates messages from ControllerLogger.
 *
 * @author Brian Stansberry
 */
@MessageLogger(projectCode = "WFLYCTL", length = 4)
public interface ControllerLoggerDuplicate extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    ControllerLoggerDuplicate ROOT_LOGGER = Logger.getMessageLogger(ControllerLoggerDuplicate.class, "org.wildfly.management");

    /**
     * Creates an exception indicating ad duplicate path element, represented by the {@code name} parameter, was found.
     *
     * @param name the name of the duplicate entry.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 71, value = "Duplicate path element '%s' found")
    OperationFailedRuntimeException duplicateElement(String name);

    /**
     * Creates an exception indicating an element, represented by the {@code name} parameter, has already been
     * declared.
     *
     * @param name     the element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 73, value = "An element of this type named '%s' has already been declared")
    XMLStreamException duplicateNamedElement(String name, @Param Location location);

    /**
     * An exception indicating the type is invalid.
     *
     * @param name        the name the invalid type was found for.
     * @param validType   the valid type.
     * @param invalidType the invalid type.
     *
     * @return the exception.
     */
    @Message(id = 97, value = "Wrong type for '%s'. Expected %s but was %s")
    OperationFailedException incorrectType(String name, ModelType validType, ModelType invalidType);

    @Message(id = NONE, value = "Couldn't convert %s to %s")
    String typeConversionError(ModelNode value, ModelType validType);

    /**
     * A message indicating the value, represented by the {@code value} parameter, is invalid and must be of the form
     * address/mask.
     *
     * @param value the invalid value.
     *
     * @return the message.
     */
    @Message(id = 102, value = "Invalid 'value' %s -- must be of the form address/mask")
    String invalidAddressMaskValue(String value);

    /**
     * A message indicating the mask, represented by the {@code mask} parameter, is invalid.
     *
     * @param mask the invalid mask.
     * @param msg  the error message.
     *
     * @return the message.
     */
    @Message(id = 103, value = "Invalid mask %s (%s)")
    String  invalidAddressMask(String mask, String msg);

    /**
     * A message indicating the address value, represented by the {@code value} parameter, is invalid.
     *
     * @param value the invalid address value.
     * @param msg   the error message.
     *
     * @return the message.
     */
    @Message(id = 104, value = "Invalid address %s (%s)")
    String invalidAddressValue(String value, String msg);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 106, value = "Invalid value '%s' for attribute '%s'")
    XMLStreamException invalidAttributeValue(String value, QName name, @Param Location location);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter. The value must be between the {@code minInclusive} and
     * {@code maxInclusive} values.
     *
     * @param value        the invalid value.
     * @param name         the attribute name.
     * @param minInclusive the minimum value allowed.
     * @param maxInclusive the maximum value allowed.
     * @param location     the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 107, value = "Illegal value %d for attribute '%s' must be between %d and %d (inclusive)")
    XMLStreamException invalidAttributeValue(int value, QName name, int minInclusive, int maxInclusive, @Param Location location);

    /**
     * Creates an exception indicating an invalid integer value, represented by the {@code value} parameter, was found
     * for the attribute, represented by the {@code name} parameter.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 108, value = "Illegal value '%s' for attribute '%s' must be an integer")
    XMLStreamException invalidAttributeValueInt(@Cause Throwable cause, String value, QName name, @Param Location location);

    /**
     * Creates an exception indicating the {@code key} is invalid.
     *
     * @param element the path element
     * @param key the invalid value.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 110, value = "Invalid resource address element '%s'. The key '%s' is not valid for an element in a resource address.")
    String invalidPathElementKey(String element, String key);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a maximum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the maximum length.
     *
     * @return the message.
     */
    @Message(id = 112, value = "'%s' is an invalid value for parameter %s. Values must have a maximum length of %d characters")
    String invalidMaxLength(String value, String name, int length);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a minimum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the minimum length.
     *
     * @return the message.
     */
    @Message(id = 113, value = "'%s' is an invalid value for parameter %s. Values must have a minimum length of %d characters")
    String invalidMinLength(String value, String name, int length);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param maxSize the maximum size allowed.
     *
     * @return the message
     */
    @Message(id = 114, value = "[%d] is an invalid size for parameter %s. A maximum length of [%d] is required")
    String invalidMaxSize(int size, String name, int maxSize);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param minSize the minimum size allowed.
     *
     * @return the message
     */
    @Message(id = 115, value = "[%d] is an invalid size for parameter %s. A minimum length of [%d] is required")
    String invalidMinSize(int size, String name, int minSize);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 116, value = "%d is an invalid value for parameter %s. A maximum value of %d is required")
    String invalidMaxValue(int value, String name, int maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMaxValue(long value, String name, long maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 117, value = "%d is an invalid value for parameter %s. A minimum value of %d is required")
    String invalidMinValue(int value, String name, int minValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMinValue(long value, String name, long minValue);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param value    the invalid value.
     * @param name     the name of the attribute.\
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 119, value = "Value %s for attribute %s is not a valid multicast address")
    OperationFailedException invalidMulticastAddress(String value, String name);

    /**
     * Creates an exception indicating the {@code value} is invalid.
     *
     * @param element the path element
     * @param value the invalid value.
     * @param character the invalid character
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 128, value = "Invalid resource address element '%s'. The value '%s' is not valid for an element in a resource address. Character '%s' is not allowed.")
    String invalidPathElementValue(String element, String value, Character character);

    /**
     * A message indicating the {@code value} for the parameter, represented by the {@code name} parameter, is invalid.
     *
     * @param value       the invalid value.
     * @param name        the name of the parameter.
     * @param validValues a collection of valid values.
     *
     * @return the message.
     */
    @Message(id = 129, value = "Invalid value %s for %s; legal values are %s")
    OperationFailedException invalidValue(String value, String name, Collection<?> validValues);

    /**
     * Creates an exception indicating there are missing required attribute(s).
     *
     * @param sb       the missing attributes.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 133, value = "Missing required attribute(s): %s")
    XMLStreamException missingRequiredAttributes(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating there are missing required element(s).
     *
     * @param sb       the missing element.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 134, value = "Missing required element(s): %s")
    XMLStreamException missingRequiredElements(StringBuilder sb, @Param Location location);

    /**
     * An exception indicating the {@code name} may not be {@code null}.
     *
     * @param name the name that cannot be {@code null}.
     *
     * @return the exception.
     */
    @Message(id = 155, value = "'%s' may not be null")
    OperationFailedException nullNotAllowed(String name);

    /**
     * Creates an exception indicating an unexpected attribute, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 197, value = "Unexpected attribute '%s' encountered")
    XMLStreamException unexpectedAttribute(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 198, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected end of an element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 199, value = "Unexpected end of element '%s' encountered")
    XMLStreamException unexpectedEndElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param validValues the legal values for the attribute
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 209, value = "Invalid value '%s' for attribute '%s' -- valid values are %s")
    XMLStreamException invalidAttributeValue(String value, QName name, Set<String> validValues, @Param Location location);

    /**
     * Creates an exception message indicating a child resource cannot be found.
     *
     * @param childAddress the address element for the child.
     *
     * @return an message for the error.
     */
    @Message(id = 217, value = "Child resource '%s' not found")
    String childResourceNotFound(AddressElement childAddress);

    /**
     * Creates an exception indicating that the value of the specified parameter does not match any of the allowed
     * values.
     *
     * @param value the parameter value.
     * @param parameterName the parameter name.
     * @param allowedValues a set containing the allowed values.
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 248, value="Invalid value %s for %s; legal values are %s")
    OperationFailedException invalidEnumValue(String value, String parameterName, Set<?> allowedValues);

    /**
     * An exception indicating the {@code name} may not be {@link ModelType#EXPRESSION}.
     *
     * @param name the name of the attribute or parameter value that cannot be an expression
     *
     * @return the exception.
     */
    @Message(id = 264, value = "%s may not be ModelType.EXPRESSION")
    OperationFailedException expressionNotAllowed(String name);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 266, value = "Value %s for attribute %s is not a valid multicast address")
    OperationFailedException unknownMulticastAddress(@Cause UnknownHostException cause, String value, String name);

    @Message(id = 283, value = "Could not marshal attribute as element: %s")
    UnsupportedOperationException couldNotMarshalAttributeAsElement(String attributeName);

    @Message(id = 284, value = "Could not marshal attribute as attribute: %s")
    UnsupportedOperationException couldNotMarshalAttributeAsAttribute(String attributeName);

    @Message(id = 341, value="A uri with bad syntax '%s' was passed for validation.")
    OperationFailedException badUriSyntax(String uri);

    @Message(id = 372, value="List attribute '%s' contains duplicates, which are not allowed")
    OperationFailedException duplicateElementsInList(String name);

    @Message(id = 376, value = "Unexpected attribute '%s' encountered. Valid attributes are: '%s'")
    XMLStreamException unexpectedAttribute(QName name, StringBuilder possibleAttributes, @Param Location location);

    @Message(id = 377, value = "Unexpected element '%s' encountered. Valid elements are: '%s'")
    XMLStreamException unexpectedElement(QName name, StringBuilder possible, @Param Location location);

    @Message(id = 387, value="Illegal path address '%s' , it is not in a correct CLI format")
    IllegalArgumentException illegalCLIStylePathAddress(String pathAddress);

    @Message(id = 390, value = "An invalid key '%s' has been supplied for parameter '%s'")
    OperationFailedException invalidKeyForObjectType(String key, String parameter);

    @Message(id = 394, value = "Capability '%s' does not provide services of type '%s'")
    IllegalArgumentException invalidCapabilityServiceType(String capabilityName, Class<?> serviceType);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a maximum bytes length, represented by the
     * {@code length} parameter.
     *
     * @param str the invalid value.
     * @param parameterName the name of the parameter.
     * @param max the maximum length.
     *
     * @return the message.
     */
    @Message(id = 419, value = "'%s' is an invalid value for parameter %s. Values must have a maximum length of %d bytes")
    String invalidMaxBytesLength(String str, String parameterName, int max);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a minimum bytes length, represented by the
     * {@code length} parameter.
     *
     * @param str the invalid value.
     * @param parameterName the name of the parameter.
     * @param min the minimum length.
     *
     * @return the message.
     */
    @Message(id = 420, value = "'%s' is an invalid value for parameter %s. Values must have a minimum length of %d bytes")
    String invalidMinBytesLength(String str, String parameterName, int min);    @Message(id = 433, value = "'%s' is not a valid representation of a resource address")
    OperationFailedException invalidAddressFormat(ModelNode address);

    @Message(id = 439, value = "Value %s for attribute %s is not a valid subnet format")
    OperationFailedException invalidSubnetFormat(String value, String name);

}
