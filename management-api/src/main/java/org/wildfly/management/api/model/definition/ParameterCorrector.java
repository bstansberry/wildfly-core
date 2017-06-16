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

import org.jboss.dmr.ModelNode;

/**
 * An implementation of this interface will be invoked before
 * a new attribute value is set, so it has a chance to adjust the new value,
 * if and as necessary, e.g.
 * <ol>
 *     <li>perform conversions from previously legal node structures to currently legal structures</li>
 *     <li>propagate properties from the current value in case the new value is missing them</li>
 * </ol>
 * <p>
 * The implementation of this interface will be invoked before the new value is validated by an
 * item's parameter validator, and the corrected value is what will be passed to the parameter validator
 * for validation.
 *
 * @author Alexey Loubyansky
 */
@FunctionalInterface
public interface ParameterCorrector {

    /**
     * Adjusts the value to be set on the attribute.
     *
     * @param newValue  the new value to be set
     * @param currentValue  the current value of the attribute
     * @return  the value that actually should be set
     */
    ModelNode correct(ModelNode newValue, ModelNode currentValue);
}
