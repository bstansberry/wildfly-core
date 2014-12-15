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

package org.wildfly.core.expressions.logging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
@MessageLogger(projectCode = "WFLYEXP", length = 4)
public interface ExpressionsLogger extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    ExpressionsLogger ROOT_LOGGER = Logger.getMessageLogger(ExpressionsLogger.class, "org.wildfly.core.expressions");

    /**
     * Creates an exception message indicating an expression could not be resolved due to no corresponding system property
     * or environment variable.
     *
     * @param toResolve  the node being resolved
     * @return an {@link IllegalStateException} for the caller
     */
    @Message(id = 1, value = "Cannot resolve expression '%s'")
    String cannotResolveExpression(String toResolve);

    @Message(id = 2, value="Incomplete expression: %s")
    String incompleteExpression(String expression);

    /**
     * Creates an exception message indicating an expression could not be resolved due to lack of security permissions.
     *
     * @param toResolve  the node being resolved
     * @param e the SecurityException
     * @return a message for the caller
     */
    @Message(id = 210, value = "Caught SecurityException attempting to resolve expression '%s' -- %s")
    String noPermissionToResolveExpression(String toResolve, SecurityException e);
}
