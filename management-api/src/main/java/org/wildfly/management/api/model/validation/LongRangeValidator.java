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

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.OperationFailedException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * Validates that the given parameter is a long in a given range.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LongRangeValidator extends ModelTypeValidator implements MinMaxValidator {
    protected final long min;
    protected final long max;

    public LongRangeValidator(final long min) {
        this(min, Long.MAX_VALUE);
    }

    public LongRangeValidator(final long min, final long max) {
        super(ModelType.LONG, false);
        this.min = min;
        this.max = max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            long val = value.asLong();
            if (val < min) {
                throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMinValue(val, parameterName, min));
            }
            else if (val > max) {
                throw new OperationFailedException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidMaxValue(val, parameterName, max));
            }
        }
    }

    @Override
    public Long getMin() {
        return min;
    }

    @Override
    public Long getMax() {
        return max;
    }
}
