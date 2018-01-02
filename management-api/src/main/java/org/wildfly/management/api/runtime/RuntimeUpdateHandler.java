/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.wildfly.management.api.runtime;

import org.wildfly.management.api.OperationClientException;

/**
 * Performs a modification of the runtime state of a managed process. Executed as an individual step
 * in the overall execution of a management operation.
 *
 * @author Brian Stansberry
 */
@FunctionalInterface
public interface RuntimeUpdateHandler {

    /**
     * Execute this step.  If the operation fails, {@link RuntimeUpdateContext#getFailureDescription() context.getFailureDescription()}
     * must be called, or an {@link OperationClientException} must be thrown.
     * If the operation succeeded and the operation provides a return value, {@link RuntimeUpdateContext#getResult() context.getResult()} should
     * be called and the result populated with the outcome. If the handler wishes to take further action once the result
     * of the overall operation execution is known, either
     * {@link RuntimeUpdateContext#setResultHandler(RuntimeUpdateContext.ResultHandler)} or
     * {@link RuntimeUpdateContext#setRollbackHandler(RuntimeUpdateContext.RollbackHandler)}
     * should be called to register a callback. The callback will not be invoked if this method throws an exception.
     * <p>When this method is invoked the {@link Thread#getContextClassLoader() thread context classloader} will
     * be set to be the defining class loader of the class that implements this interface.</p>
     *
     * @param context the operation context
     * @throws OperationClientException if the operation failed <b>before</b> calling {@code context.completeStep()}
     */
    void execute(RuntimeUpdateContext context) throws OperationClientException;
}
