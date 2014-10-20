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

package org.jboss.as.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an error condition encountered when executing a management operation.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public final class OperationErrorCode {

    /**
     * Enumeration of standard {@link org.jboss.as.controller.OperationErrorCode}s.
     * <p>
     * <strong>Note:</strong> {@link org.jboss.as.controller.OperationErrorCode} itself is not
     * an enum to provide to possibility for custom error codes besides these standard ones.
     */
    public enum StandardErrorCodes {

        /** The request was faulty in some way other than the other client errors enumerated in this enum. */
        BAD_REQUEST(400000),
        /** The request targets a non-existent management resource */
        RESOURCE_NOT_FOUND(404000),
        /** The request targets a non-existent management resource */
        NO_SUCH_ATTRIBUTE(404001),
        /** The request targets a non-existent management operation */
        NO_SUCH_OPERATION(404002),
        /** The caller is not authorized to perform the requested action */
        UNAUTHORIZED(403000),
        /** An unexpected error occurred */
        INTERNAL_SERVER_ERROR(500000),
        /**
         * Execution of the request was cancelled due to some administrative action.
         * We use a code for this that is in the range for "server faults" (e.g. HTTP 5xx response codes)
         * because from the point of view of the caller of this specific operation, the server
         * did not handle the request as expected but did not complain about the request itself.
         */
        CANCELLED(500001),
        /** Execution of the request was not proceeding in a timely manner and was aborted*/
        EXECUTION_TIMEOUT(500002),
        /** The management services are not presently available to callers. */
        MANAGEMENT_UNAVAILABLE(503000);

        private final OperationErrorCode errorCode;

        StandardErrorCodes(int errorCode) {
            this.errorCode = new OperationErrorCode(errorCode);
        }

        /**
         * Gets the actual {@link org.jboss.as.controller.OperationErrorCode}
         * @return the error code. Will not return {@code null}
         */
        public OperationErrorCode getOperationErrorCode() {
            return errorCode;
        }

        /**
         * Gets {@link #getCode() int representation} of the error code.
         *
         * @return the code
         */
        public int getCode() {
            return errorCode.getCode();
        }
    }

    private static final Map<Integer, OperationErrorCode> codesByInt = new HashMap<>();
    static {
        for (StandardErrorCodes code : StandardErrorCodes.values()) {
            OperationErrorCode oec = code.getOperationErrorCode();
            codesByInt.put(oec.getCode(), oec);
        }
    }

    /**
     * Gets the {@code OperationErrorCode} whose {@link #getCode() basic code} matches
     * the {@code code} param
     * @param code  the error code
     * @return the matching {@code OperationErrorCode}. Will not return null
     *
     * @throws java.lang.IllegalArgumentException if no matching code exists
     */
    public static OperationErrorCode forCode(int code) {
        OperationErrorCode result = codesByInt.get(code);
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    private final int code;
    private final int httpGetCode;
    private final int httpPostCode;
    private final boolean requestError;

    private OperationErrorCode(int code) {
        assert code > 99999 && code < 1000000;
        this.code = code;
        this.httpGetCode = code / 1000;
        this.httpPostCode = httpGetCode == 404 ? 400 : httpGetCode;
        this.requestError = code < 500000;
    }

    /**
     * Gets the basic integer representation of this error code. This is the value
     * that would be used in the {@code error-code} field in an operation response {@code ModelNode}
     * and is the value to pass to {@link #forCode(int)} to retrieve this object.
     *
     * @return the integer representation of this error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the HTTP response code that should be used to represent this error code in HTTP
     * responses to GET requests and other HTTP requests where the request URI encodes the target
     * of the request.
     *
     * @return  the http response code
     */
    public int getHttpGetResponseCode() {
        return httpGetCode;
    }

    /**
     * Gets the HTTP response code that should be used to represent this error code in HTTP
     * responses to POST requests and other HTTP requests where the request URI does not encode
     * the target of the request, but rather the target is determined by reading the request entity.
     *
     * @return  the http response code
     */
    public int getHttpPostResponseCode() {
        return httpPostCode;
    }

    /**
     * Gets whether this error code indicates an problem with the request.
     *
     * @return {@code true} if the error was a problem with the request; {@code false} if it relates to
     *         a server-side problem executing the request
     */
    public boolean isRequestError() {
        return requestError;
    }
}
