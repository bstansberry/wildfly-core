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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;
import org.wildfly.management.api.model.validation.AllowedValuesValidator;
import org.wildfly.management.api.model.validation.ModelTypeValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * {@link ParameterValidator} that validates the value is a string matching
 * one of the {@link Enum} types.
 *
 * @author Jason T. Greene
 * @author Brian Stansberry
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class EnumValidator<E extends Enum<E>> extends ModelTypeValidator implements AllowedValuesValidator {

    private final EnumSet<E> allowedValues;
    private final Class<E> enumType;
    private final Map<String, E> toStringMap = new HashMap<String, E>();

    /**
     * Creates a validator where the specified enum values are allowed
     * @param enumType the type of the enum
     * @param allowed  the allowed values. Cannot be {@code null} or empty
     * @param safe {@code true} if the inputs have been validated and the set is an internally created one
     */
    private EnumValidator(final Class<E> enumType, final EnumSet<E> allowed, boolean safe) {
        super(ModelType.STRING);
        if (safe) {
            this.enumType = enumType;
            this.allowedValues = allowed;
        } else {
            assert enumType != null;
            assert allowed != null;
            this.enumType = enumType;
            this.allowedValues = EnumSet.copyOf(allowed);
        }
        for (E value : allowedValues) {
            toStringMap.put(value.toString(), value);
        }
    }

    /**
     * Creates a new validator for the enum type with the allowed values equal to all values of the enum.
     *
     * @param enumType the type of the enum.
     * @param <E>      the type of the enum.
     *
     * @return a new validator.
     */
    static <E extends Enum<E>> EnumValidator<E> create(final Class<E> enumType) {
        assert enumType != null;
        EnumSet<E> set = EnumSet.allOf(enumType);
        return new EnumValidator<E>(enumType, set, true);
    }

    /**
     * Creates a new validator for the enum type with the allowed values defined in the {@code allowed} parameter.
     *
     * @param allowed  the enum values that are allowed. Cannot be {@code null} or have zero elements
     * @param <E>      the type of the enum.
     *
     * @return a new validator.
     */
    @SafeVarargs
    static <E extends Enum<E>> EnumValidator<E> create(final E... allowed) {
        assert allowed != null;
        assert allowed.length > 0;
        Class<E> enumType = allowed[0].getDeclaringClass();
        EnumSet<E> set;
        if (allowed.length == 1) {
            set = EnumSet.of(allowed[0]);
        } else {
            set = EnumSet.noneOf(enumType);
            Collections.addAll(set, allowed);
        }
        return new EnumValidator<E>(enumType, set, true);
    }

    /**
     * Creates a new validator for the enum type with the allowed values defined in the {@code allowed} parameter.
     *
     * @param enumType the type of the enum.
     * @param allowed  the enum values that are allowed.
     * @param <E>      the type of the enum.
     *
     * @return a new validator.
     */
    static <E extends Enum<E>> EnumValidator<E> create(final Class<E> enumType, final EnumSet<E> allowed) {
        return new EnumValidator<E>(enumType, allowed, false);
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationClientException {
        super.validateParameter(parameterName, value);

        String tuString = value.asString(); // Sorry, no support for resolving against vault!
        E enumValue;
        try {
            enumValue = Enum.valueOf(enumType, tuString.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            // valueof failed - are we using the toString representation of the Enum type?
            enumValue = toStringMap.get(tuString);
        }
        if (enumValue == null || !allowedValues.contains(enumValue)) {
            throw ControllerLoggerDuplicate.ROOT_LOGGER.invalidEnumValue(tuString, parameterName, toStringMap.keySet());
        }

        // Hack to store the allowed value in the model, not the user input
        try {
            value.set(enumValue.toString());
        } catch (Exception e) {
            // node must be protected.
        }
    }

    @Override
    public boolean replacesDefaultValidation() {
        // We replace default validation because we do case-insensitive checks
        return true;
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        List<ModelNode> result = new ArrayList<ModelNode>();
        for (E value : allowedValues) {
            if (value.toString() != null) {
                result.add(new ModelNode().set(value.toString()));
            } else {
                result.add(new ModelNode().set(value.name()));
            }
        }
        return result;
    }
}
