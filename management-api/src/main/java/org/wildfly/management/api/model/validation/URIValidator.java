/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.OperationFailedException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * A {@link ParameterValidator} to verify that a parameter is a correctly formed URI.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class URIValidator implements ParameterValidator, MinMaxValidator {

    public static final URIValidator INSTANCE = new URIValidator();

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {

        String str = value.asString();

        try {
            new URI(str);
        } catch (URISyntaxException e) {
            throw ControllerLoggerDuplicate.ROOT_LOGGER.badUriSyntax(str);
        }
    }

    @Override
    public Long getMin() {
        return 1L;
    }

    @Override
    public Long getMax() {
        return MAX_INT;
    }

}
