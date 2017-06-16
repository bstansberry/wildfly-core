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

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Contextual information and operations made available to a {@link RuntimeReadHandler} as it executes.
 *
 * @author Brian Stansberry
 */
public interface RuntimeReadContext extends RuntimeOperationContext {

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(RuntimeReadHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @param addFirst add the handler before the others
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(RuntimeReadHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process, writing any output to the response object
     * associated with the current step.
     * Runtime operation steps are automatically added after configuration operation steps.  Since only one operation
     * may perform runtime work at a time, this method may block until other runtime operations have completed.
     *
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(final ModelNode operation, final RuntimeReadHandler step, final Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process, writing any output to the response object
     * associated with the current step.
     * Runtime operation steps are automatically added after configuration operation steps.  Since only one operation
     * may perform runtime work at a time, this method may block until other runtime operations have completed.
     *
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @param addFirst add the handler before the others
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(final ModelNode operation, final RuntimeReadHandler step, final Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param response the response which the nested step should populate
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(ModelNode response, ModelNode operation, RuntimeReadHandler step, Stage stage) throws IllegalArgumentException;

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param response the response which the nested step should populate
     * @param operation the operation body to pass into the added step
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @param addFirst add the handler before the others
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(ModelNode response, ModelNode operation, RuntimeReadHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Register for
     * {@link RuntimeReadContext.ResultHandler#handleResult(RuntimeOperationContext.ResultAction, RuntimeReadContext) a notification}
     * when the overall result of the operation is known. Handlers that register for notifications will receive the
     * notifications in the reverse of the order in which their steps execute.
     *
     * @param resultHandler the handler for the result notification. Cannot be {@code null}.
     *
     * @throws IllegalStateException if a result handler has already been set
     */
    void setResultHandler(RuntimeReadContext.ResultHandler resultHandler);

    /**
     * Get the service registry.  The returned registry must not be used to remove services and if an attempt is made
     * to call {@code ServiceController.setMode(REMOVE)} on a {@code ServiceController} returned from this registry an
     * {@code IllegalStateException} will be thrown. Callers also <strong>MUST NOT</strong> make any modifications to
     * any {@code ServiceController} or {@code Service} implementation obtained via this registry. A
     * {@link RuntimeReadHandler} should only perform reads.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Handler for a callback to a {@link RuntimeReadHandler} indicating that the result of the overall operation is
     * known and the handler can take any necessary actions to deal with that result.
     */
    @FunctionalInterface
    interface ResultHandler {

        /**
         * Callback to an {@link RuntimeReadHandler} indicating that the result of the overall operation is
         * known and the handler can take any necessary actions to deal with that result.
         *
         * @param resultAction the overall result of the operation
         * @param context  the operation execution context; will be the same as what was passed to the
         *                 {@link RuntimeReadHandler#execute(RuntimeReadContext)} method invocation
         *                 that registered this rollback handler.
         */
        void handleResult(RuntimeOperationContext.ResultAction resultAction, RuntimeReadContext context);
    }
}
