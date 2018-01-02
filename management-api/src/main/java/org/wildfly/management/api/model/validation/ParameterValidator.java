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
import org.wildfly.management.api.OperationClientException;

/**
 * Performs validation on detyped operation parameters.
 * <p>
 * <strong>Expression and undefined value checking:</strong> A custom {@code ParameterValidator} implementation
 * should not perform validation of values of type {@link org.jboss.dmr.ModelType#EXPRESSION} or
 * {@link org.jboss.dmr.ModelType#UNDEFINED}, as that will have already been checked before any custom validator
 * is invoked. The custom validator will not be invoked if the value is one of those types.
 *
 * @author Brian Stansberry
 */
public interface ParameterValidator {

    /**
     * Validate the parameter with the given name.
     *
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @param value the parameter value. Cannot be {@code null}
     *
     * @throws OperationClientException if the value is not valid
     */
    void validateParameter(String parameterName, ModelNode value) throws OperationClientException;

    /**
     * Gets whether the standard validation performed based on the settings in an
     * {@link org.wildfly.management.api.model.definition.ItemDefinition} should not be performed before this
     * validator is invoked. If {@code false} (which is the default) this validator will not be invoked unless
     * the value has already passed the standard validation. Note that this setting does not disable checks
     * for valid {@code undefined} or expression values, which are always performed.
     *
     * @return {@code true} if the standard checks should not be performed.
     */
    default boolean replacesDefaultValidation() {
        return false;
    }
}
