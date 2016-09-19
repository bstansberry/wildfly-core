/*
Copyright 2016 Red Hat, Inc.

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

package org.jboss.as.host.controller.mgmt;

import java.util.List;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Wrapper to the DomainController and the underlying {@code ModelController} to execute
 * a {@code OperationStepHandler} implementation directly, bypassing normal domain coordination layer.
 * TODO This interface probably should be adapted to provide use-case-specific methods instead of generic
 * "execute whatever I hand you" ones. The "installSlaveExtensions" method needed to be non-generic unless
 * I was willing to hand a ref to the root MRR to RemoteDomainConnnectionService.
 */
public interface HostControllerOperationExecutor {

    /**
     * Execute the operation.
     *
     * @param operation operation
     * @param handler the message handler
     * @param control the transaction control
     * @param step the step to be executed
     * @return the result
     */
    ModelNode execute(Operation operation, OperationMessageHandler handler, ModelController.OperationTransactionControl control, OperationStepHandler step);

    /**
     * Execute the operation to install extensions provided by a remote domain controller.
     *
     *
     * @param extensions@return the result
     */
    ModelNode installSlaveExtensions(List<ModelNode> extensions);

    /**
     * Execute an operation using the current management model.
     *
     * @param operation    the operation
     * @param handler      the operation handler to use
     * @return the operation result
     */
    ModelNode executeReadOnly(ModelNode operation, OperationStepHandler handler, ModelController.OperationTransactionControl control);

    /**
     * Execute an operation using given resource model.
     *
     * @param operation    the operation
     * @param model        the resource model
     * @param handler      the operation handler to use
     * @return the operation result
     */
    ModelNode executeReadOnly(ModelNode operation, Resource model, OperationStepHandler handler, ModelController.OperationTransactionControl control);

    /**
     * Attempts to acquire a non-exclusive read lock. After any operations requiring this lock have completed, #releaseReadlock
     * must be called to release the lock.
     * @param operationID - the operationID for this registration. Cannot be {@code null}.
     * @throws IllegalArgumentException - if operationID is null.
     * @throws InterruptedException - if the lock is not acquired.
     */
    void acquireReadlock(final Integer operationID) throws IllegalArgumentException, InterruptedException;

    /**
     * Release the non-exclusive read lock obtained from #acquireReadlock. This method must be called after any locked
     * operations have completed or aborted to release the shared lock.
     * @param operationID - the operationID for this registration. Cannot be {@code null}.
     * @throws IllegalArgumentException - if if the operationId is null.
     * @throws IllegalStateException - if the shared lock was not held.
     */
    void releaseReadlock(final Integer operationID) throws IllegalArgumentException;
}
