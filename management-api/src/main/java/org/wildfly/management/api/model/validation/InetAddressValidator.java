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
import org.wildfly.common.net.Inet;
import org.wildfly.management.api.OperationFailedException;

/**
 * Validates that the given parameter is a string that can be converted into an InetAddress.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@SuppressWarnings("unused")
public final class InetAddressValidator implements ParameterValidator {

    public static final InetAddressValidator INSTANCE = new InetAddressValidator();

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        String str = value.asString();
        if (Inet.parseInetAddress(str) == null) {
            throw new OperationFailedException("Address is invalid: \"" + str + "\"");
        }
    }

}
