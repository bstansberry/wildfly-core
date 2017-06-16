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

package org.wildfly.management.api.model.validation;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.OperationFailedException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * Validates that a parameter is a byte[] of an acceptable length.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BytesValidator extends ModelTypeValidator implements MinMaxValidator {

    public static BytesValidator createSha1() {
        return new BytesValidator(20, 20);
    }

    private final int min;
    private final int max;

    /**
     * Creates a BytesValidator that allows potentially more than one type.
     * @param min the minimum length of the byte[]
     * @param max the maximum length of the byte[]
     */
    public BytesValidator(final int min, final int max) {
        super(ModelType.BYTES);
        this.min = min;
        this.max = max;
    }

    /**
     * Creates a BytesValidator
     * @param min the minimum length of the byte[]
     * @param max the maximum length of the byte[]
     * @param otherValidTypes additional valid types (i.e one of those whose value can convert to a byte[].) May be {@code null}
     */
    public BytesValidator(final int min, final int max, final ModelType... otherValidTypes) {
        super( false, ModelType.BYTES, otherValidTypes);
        this.min = min;
        this.max = max;
    }

    @Override
    public Long getMin() {
        return (long) min;
    }

    @Override
    public Long getMax() {
        return (long) max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            byte[] val = value.asBytes();
            if (val.length < min) {
                throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinSize(val.length, parameterName, min));
            }
            else if (val.length > max) {
                throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxSize(val.length, parameterName, max));
            }
        }
    }
}
