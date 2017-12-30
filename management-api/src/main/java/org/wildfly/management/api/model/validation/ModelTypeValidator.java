/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.management.api.model.validation;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.OperationFailedException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * Base class for custom validators that perform the basic function of validating that the given parameter is
 * of the correct type. This class is public but only has protected constructors because the only reason to
 * instantiate an instance of this class is to perform additional validation via a subclass.
 * <p>
 * Note on type matching:
 * </p>
 * <p>
 * The constructor takes a parameter {@code strictType}. If {@code strictType} is {@code true}, nodes being validated do
 * not need to precisely match the type(s) passed to the constructor; rather a limited set of value conversions
 * will be attempted, and if the node value can be converted, the node is considered to match the required type.
 * The conversions are:
 * <ul>
 * <li>For BIG_DECIMAL, BIG_INTEGER, DOUBLE, INT, LONG and PROPERTY, the related ModelNode.asXXX() method is invoked; if
 * no exception is thrown the type is considered to match. For INT and LONG the numeric value must also fit in the<
 * legal range for an int or a long respectively./li>
 * <li>For BOOLEAN, if the node is of type BOOLEAN it is considered to match. If it is of type STRING with a value
 * ignoring case of "true" or "false" it is considered to match.</li>
 * <li>For OBJECT, if the node is of type OBJECT or PROPERTY it is considered to match. If it is of type LIST and each element
 * in the list is of type PROPERTY it is considered to match.</li>
 * <li>For STRING, if the node is of type STRING, BIG_DECIMAL, BIG_INTEGER, DOUBLE, INT or LONG it is considered to match.</li>
 * </ul>
 * For all other types, an exact match is required.
 * </p>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModelTypeValidator implements ParameterValidator {
    private static final BigDecimal BIGDECIMAL_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal BIGDECIMAL_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigInteger BIGINTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIGINTEGER_MIN = BigInteger.valueOf(Integer.MIN_VALUE);

    private final ModelType validType;
    private final boolean strictType;

    /**
     * Creates a ModelTypeValidator that allows the given type with non-strict checking.
     * Same as {@code ModelTypeValidator(type, false)}. This constructor is protected because the only reason to
     * instantiate an instance of this class is to perform additional validation via a subclass.
     *
     * @param type the valid type. Cannot be {@code null}
     */
    protected ModelTypeValidator(final ModelType type) {
        this(type, false);
    }

    /**
     * Creates a ModelTypeValidator that allows the given type. This constructor is protected because the only reason to
     * instantiate an instance of this class is to perform additional validation via a subclass.
     *
     * @param type the valid type. Cannot be {@code null}
     * @param strictType {@code true} if the type of a value must precisely match {@code type}; {@code false} if the value
     *              conversions described in the class javadoc can be performed to check for compatible types
     */
    protected ModelTypeValidator(final ModelType type, final boolean strictType) {
        this.validType = type;
        this.strictType = strictType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        validateType(parameterName, value, validType, strictType);
    }

    /**
     * Validates if the given {@code value} has a type consistent with the given {@code validType}, throwing
     * an {@link OperationFailedException} if not. See "Note on type matching" in the class documentation
     * for a description of the matching algorithm.
     * <p>
     * An {@link ModelNode#isDefined()} undefined} value will throw an OFE unless {@code validType} is {@link ModelType#UNDEFINED}.
     *
     * @param parameterName the name of the parameter being validated. Cannot be {@code null}
     * @param value         the value being validated. Cannot be {@code null}
     * @param validType     the legal type for the value. Cannot be {@code null}
     * @param strictType    {@code true} if the {@link ModelNode#getType() type of the value} must exactly match {@code validType}; {@code false} if various type conversions can be attempted
     * @throws OperationFailedException if the value is not valid
     */
    public static void validateType(String parameterName, ModelNode value, ModelType validType, boolean strictType) throws OperationFailedException {
        ModelType valueType = value.getType();
        if (validType != valueType) {
            try {
                if (strictType || !matches(value, valueType, validType)) {
                    throw ControllerLoggerDuplicate.ROOT_LOGGER.incorrectType(parameterName, validType, valueType);
                }
            } catch (RuntimeException e) {
                String message = String.format("%s. %s",
                        ControllerLoggerDuplicate.ROOT_LOGGER.incorrectType(parameterName, validType, valueType).getLocalizedMessage(),
                        ControllerLoggerDuplicate.ROOT_LOGGER.typeConversionError(value, validType));
                throw new OperationFailedException(message, e);
            }
        }
    }

    private static boolean matches(ModelNode value, ModelType valueType, ModelType validType) {

        switch (validType) {
            case BIG_DECIMAL: {
                value.asBigDecimal();
                return true;
            }
            case BIG_INTEGER: {
                value.asBigInteger();
                return true;
            }
            case DOUBLE: {
                value.asDouble();
                return true;
            }
            case INT: {
                switch (valueType) {
                    case BIG_DECIMAL:
                        BigDecimal valueBigDecimal = value.asBigDecimal();
                        return (valueBigDecimal.compareTo(BIGDECIMAL_MAX) <= 0) && (valueBigDecimal.compareTo(BIGDECIMAL_MIN) >= 0);
                    case BIG_INTEGER:
                        BigInteger valueBigInteger = value.asBigInteger();
                        return (valueBigInteger.compareTo(BIGINTEGER_MAX) <= 0) && (valueBigInteger.compareTo(BIGINTEGER_MIN) >= 0);
                    case LONG:
                        Long valueLong = value.asLong();
                        return valueLong <= Integer.MAX_VALUE && valueLong >= Integer.MIN_VALUE;
                    case DOUBLE:
                        Double valueDouble = value.asDouble();
                        return valueDouble <= Integer.MAX_VALUE && valueDouble >= Integer.MIN_VALUE;
                    case STRING:
                        value.asInt();
                        return true;
                    default:
                        return false;
                }
            }
            case LONG: {
                switch (valueType) {
                    case BIG_DECIMAL:
                        BigDecimal valueBigDecimal = value.asBigDecimal();
                        return (valueBigDecimal.compareTo(BIGDECIMAL_MAX) <= 0) && (valueBigDecimal.compareTo(BIGDECIMAL_MIN) >= 0);
                    case BIG_INTEGER:
                        BigInteger valueBigInteger = value.asBigInteger();
                        return (valueBigInteger.compareTo(BIGINTEGER_MAX) <= 0) && (valueBigInteger.compareTo(BIGINTEGER_MIN) >= 0);
                    case DOUBLE:
                        Double valueDouble = value.asDouble();
                        return valueDouble <= Long.MAX_VALUE && valueDouble >= Long.MIN_VALUE;
                    case INT:
                        value.asLong();
                        return true;
                    case STRING:
                        value.asLong();
                        return true;
                    default:
                        return false;
                }
            }
            case PROPERTY: {
                value.asProperty();
                return true;
            }
            case BOOLEAN: {
                // Allow some type conversions, not others.
                switch (valueType) {
                    case STRING: {
                        String s = value.asString();
                        if ("false".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s)) {
                            return true;
                        }
                        // throw a RuntimeException to trigger the catch block in the caller
                        // that results in the added typeConversionError message
                        throw new RuntimeException();
                    }
                }
                return false;
            }
            case OBJECT: {
                // We accept OBJECT, PROPERTY or LIST where all elements are PROPERTY
                switch (valueType) {
                    case PROPERTY:
                        return true;
                    case LIST: {
                        for (ModelNode node : value.asList()) {
                            if (node.getType() != ModelType.PROPERTY) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
            case STRING: {
                // Allow some type conversions, not others.
                switch (valueType) {
                    case BIG_DECIMAL:
                    case BIG_INTEGER:
                    case BOOLEAN:
                    case DOUBLE:
                    case INT:
                    case LONG:
                        return true;
                }
                return false;
            }
            case BYTES:
            // we could handle STRING but IMO if people want to allow STRING to byte[] conversion
            // they should use a different validator class
            case LIST:
            // we could handle OBJECT but IMO if people want to allow OBJECT to LIST conversion
            // they should use a different validator class
            case EXPRESSION:
            case TYPE:
            case UNDEFINED:
            default:
                return false;
        }
    }

}
