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

/**
 * Validates that a parameter is a byte[] of an acceptable length to represent a SHA1 hash.
 *
 * @author Brian Stansberry
 */
@SuppressWarnings("unused")
public final class Sha1Validator implements ParameterValidator, MinMaxValidator {

    public static final Sha1Validator INSTANCE = new Sha1Validator();

    /**
     * Creates a Sha1Validator.
     */
    private Sha1Validator() {
    }

    @Override
    public Long getMin() {
        return 20L;
    }

    @Override
    public Long getMax() {
        return 30L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) {
        // no-op. This class is kind of just a lame way to configure the min and max for standard validation
        // so we don't need to do anything beyond that here.
    }
}
