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

package org.jboss.as.controller._private;

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.OperationClientException;
import org.jboss.as.controller.OperationErrorCode;
import org.jboss.dmr.ModelNode;

/**
 * {@link CancellationException} variant that implements {@link OperationClientException}.
 * We treat this as a "client exception" internally because the cancellation is due to
 * an administrative action and we do not want it caught and logged server-side as a server failure.
 * However, the error code we use indicates a server issue, as from the point of view of the caller
 * who invoked the operation, the server did not successfully handle the request and also did
 * not find any flaw in what the client submitted.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationCancellationException extends CancellationException implements OperationClientException {

    private static final long serialVersionUID = 0;

    public OperationCancellationException(String message) {
        super(message);
        assert message != null : "message is null";
    }

    @Override
    public ModelNode getFailureDescription() {
        return new ModelNode(getMessage());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.jboss.as.controller.OperationErrorCode.StandardErrorCodes#CANCELLED}
     */
    @Override
    public OperationErrorCode getErrorCode() {
        return OperationErrorCode.StandardErrorCodes.CANCELLED.getOperationErrorCode();
    }
}
