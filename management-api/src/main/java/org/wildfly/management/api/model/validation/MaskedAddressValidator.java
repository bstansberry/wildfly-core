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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * Validates that the given parameter is a string that can be converted into a masked InetAddress.
 *
 * @author Jason T, Greene
 */
@SuppressWarnings("unused")
public final class MaskedAddressValidator implements ParameterValidator {

    public static final MaskedAddressValidator INSTANCE = new MaskedAddressValidator();

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationClientException {
        final String[] split = value.asString().split("/");
        if (split.length != 2) {
            throw new OperationClientException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidAddressMaskValue(value.asString()));
        }
        try {
            // TODO - replace with non-dns routine
            InetAddress address = InetAddress.getByName(split[0]);
            int mask = Integer.parseInt(split[1]);

            int max = address.getAddress().length * 8;
            if (mask > max) {
                throw new OperationClientException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidAddressMask(split[1], "> " + max));
            } else if (mask < 0) {
                throw new OperationClientException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidAddressMask(split[1], "< 0"));
            }
        } catch (final UnknownHostException e) {
            throw new OperationClientException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidAddressValue(split[0], e.getLocalizedMessage()));
        } catch (final NumberFormatException e) {
            throw new OperationClientException(ControllerLoggerDuplicate.ROOT_LOGGER.invalidAddressMask(split[1], e.getLocalizedMessage()));
        }
    }
}
