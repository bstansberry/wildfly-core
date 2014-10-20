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

package org.jboss.as.controller._private;

import org.jboss.as.controller.OperationErrorCode;
import org.jboss.as.controller.OperationException;
import org.jboss.dmr.ModelNode;

/**
 * Exception thrown due to detection of a request not being able to progress in a timely manner.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class OperationTimeoutException extends RuntimeException implements OperationException {

    private final ModelNode failureDescription;

    private static final long serialVersionUID = -1896884563520054972L;

    /**
     * Constructs a {@code OperationTimeoutException} with the given message.
     * The message is also used as the {@link #getFailureDescription() failure description}.
     * The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param message the description of the failure. Cannot be {@code null}
     */
    public OperationTimeoutException(final String message) {
        super(message);
        assert message != null : "message is null";
        failureDescription = new ModelNode(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getFailureDescription() {
        return failureDescription;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.jboss.as.controller.OperationErrorCode.StandardErrorCodes#EXECUTION_TIMEOUT}
     */
    @Override
    public OperationErrorCode getErrorCode() {
        return OperationErrorCode.StandardErrorCodes.EXECUTION_TIMEOUT.getOperationErrorCode();
    }
}
