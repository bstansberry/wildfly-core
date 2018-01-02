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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.capability.CapabilityServiceSupport;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.ResourceType;

/**
 * The context for execution of an operation during the portion of overall execution
 * that is concerned with runtime services.
 *
 * @author Brian Stansberry
 */
@SuppressWarnings("unused")
public interface RuntimeUpdateContext extends RuntimeOperationContext {

    /**
     * Add an execution step to this operation process.  Runtime operation steps are automatically added after
     * configuration operation steps.  Since only one operation may perform runtime work at a time, this method
     * may block until other runtime operations have completed.
     *
     * @param step the step to add
     * @param stage the stage at which the operation applies
     * @throws IllegalArgumentException if the step handler is not valid for this controller type
     */
    void addStep(RuntimeUpdateHandler step, Stage stage) throws IllegalArgumentException;

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
    void addStep(RuntimeUpdateHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

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
    void addStep(final ModelNode operation, final RuntimeUpdateHandler step, final Stage stage) throws IllegalArgumentException;

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
    void addStep(final ModelNode operation, final RuntimeUpdateHandler step, final Stage stage, boolean addFirst) throws IllegalArgumentException;

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
    void addStep(ModelNode response, ModelNode operation, RuntimeUpdateHandler step, Stage stage) throws IllegalArgumentException;

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
    void addStep(ModelNode response, ModelNode operation, RuntimeUpdateHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException;

    /**
     * Complete a step, while registering for
     * {@link RuntimeUpdateContext.RollbackHandler#handleRollback(RuntimeUpdateContext) a notification} if the work done by the
     * caller needs to be rolled back}.
     *
     * @param rollbackHandler the handler for any rollback notification. Cannot be {@code null}.
     *
     * @throws IllegalStateException if a rollback handler or a {@link #setResultHandler(RuntimeUpdateContext.ResultHandler) result handler}
     *                               has already been set
     */
    void setRollbackHandler(RuntimeUpdateContext.RollbackHandler rollbackHandler);

    /**
     * Register for
     * {@link RuntimeUpdateContext.ResultHandler#handleResult(RuntimeOperationContext.ResultAction, RuntimeUpdateContext) a notification} when the overall
     * result of the operation is known. Handlers that register for notifications will receive the notifications in
     * the reverse of the order in which their steps execute.
     *
     * @param resultHandler the handler for the result notification. Cannot be {@code null}.
     *
     * @throws IllegalStateException if a result handler or a {@link RuntimeUpdateContext#setRollbackHandler(RuntimeUpdateContext.RollbackHandler)} rollback handler}
     *                               has already been set
     */
    void setResultHandler(RuntimeUpdateContext.ResultHandler resultHandler);

    /**
     * Gets whether {@link Stage#RUNTIME} handlers can restart (or remove) runtime services in order to
     * make the operation take effect. If {@code false} and the operation cannot be effected without restarting
     * or removing services, the handler should invoke {@link #reloadRequired()} or {@link #restartRequired()}.
     *
     * @return {@code true} if a service restart or removal is allowed
     */
    boolean isResourceServiceRestartAllowed();

    /**
     * Notify the context that the process requires a stop and re-start of its root service (but not a full process
     * restart) in order to ensure stable operation and/or to bring its running state in line with its persistent configuration.
     */
    void reloadRequired();

    /**
     * Notify the context that the process must be terminated and replaced with a new process in order to ensure stable
     * operation and/or to bring the running state in line with the persistent configuration.
     */
    void restartRequired();

    /**
     * Notify the context that a previous call to {@link #reloadRequired()} can be ignored (typically because the change
     * that led to the need for reload has been rolled back.)
     */
    void revertReloadRequired();

    /**
     * Notify the context that a previous call to {@link #restartRequired()} can be ignored (typically because the change
     * that led to the need for restart has been rolled back.)
     */
    void revertRestartRequired();

    /**
     * Notify the context that an update to the runtime that would normally have been made could not be made due to
     * the current state of the process. As an example, a step handler that can only update the runtime when
     * {@link #isBooting()} is {@code true} must invoke this method if it is executed when {@link #isBooting()}
     * is {@code false}.
     */
    void runtimeUpdateSkipped();

    /**
     * Get a mutable view of the managed resource registration.  The registration is relative to the operation address.
     *
     * @return the model node registration
     * @throws IllegalStateException if the {@link ResourceType} at the given address has a
     *                               {@link ResourceType#getPathAddress() registered address} that is not a
     *                               {@link AddressElement#isWildcard()} wildcard address or if it is a
     *                               {@link ResourceType#isRuntimeOnly() runtime-only} resource
     *
    OverridableManagementResourceType getResourceRegistrationForOverride(PathAddress pathAddress);

    /**
     * Get the service registry.  The returned registry must not be used to remove services and if an attempt is made
     * to call {@code ServiceController.setMode(REMOVE)} on a {@code ServiceController} returned from this registry an
     * {@code IllegalStateException} will be thrown. To remove a service use {@link #removeService(org.jboss.msc.service.ServiceName)}.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Initiate a service removal.  If the step is not a runtime operation handler step, an exception will be thrown.  Any
     * subsequent step which attempts to add a service with the same name will block until the service removal completes.
     * The returned controller may be used to attempt to cancel a removal in progress.
     *
     * @param name the service to remove
     * @return the controller of the service to be removed if service of given name exists; null otherwise
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException;

    /**
     * Initiate a service removal.  If the step is not a runtime operation handler step, an exception will be thrown.  Any
     * subsequent step which attempts to add a service with the same name will block until the service removal completes.
     *
     * @param controller the service controller to remove
     * @throws UnsupportedOperationException if the calling step is not a runtime operation step
     */
    void removeService(ServiceController<?> controller) throws UnsupportedOperationException;

    /**
     * Get the service target.  The returned service target is limited such that only the service add methods
     * are supported.  If a service added to this target was removed by a prior operation step, the install will wait until the removal completes.
     *
     * @return the service target
     */
    CapabilityServiceTarget getCapabilityServiceTarget();

    /**
     * Marks a resource to indicate that it's backing service(s) will be restarted.
     * This is to ensure that a restart only occurs once, even if there are multiple updates.
     * When true is returned the caller has "acquired" the mark and should proceed with the
     * restart, when false, the caller should take no additional action.
     *
     * The passed owner is compared by instance when a call to {@link #revertReloadRequired()}.
     * This is to ensure that only the "owner" will be successful in reverting the mark.
     *
     * @param resource the resource that will be restarted
     * @param owner the instance representing ownership of the mark
     * @return true if the mark was required and the service should be restarted,
     *         false if no action should be taken.
     */
    boolean markResourceRestarted(ResourceAddress resource, Object owner);

    /**
     * Removes the restarted marking on the specified resource, provided the passed owner is the one
     * originally used to obtain the mark. The purpose of this method is to facilitate rollback processing.
     * Only the "owner" of the mark should be the one to revert the service to a previous state (once again
     * restarting it).When true is returned, the caller must take the required corrective
     * action by restarting the resource, when false is returned the caller should take no additional action.
     *
     * The passed owner is compared by instance to the one provided in {@link #markResourceRestarted(ResourceAddress, Object)}
     *
     * @param resource the resource being reverted
     * @param owner the owner of the mark for the resource
     * @return true if the caller owns the mark and the service should be restored by restarting
     *         false if no action should be taken.
     */
    boolean revertResourceRestarted(ResourceAddress resource, Object owner);

    /**
     * Requests that one of a capability's optional requirements hereafter be treated as required, until the process is
     * stopped or reloaded. This request will only be granted if the required capability is already present; otherwise
     * an {@link OperationClientException} will be thrown.
     * <p>
     * This method should be used only if the caller is not sure whether the capability is required until
     * a {@link RuntimeReadHandler} or a {@link RuntimeUpdateHandler} executes.
     * <strong>Not knowing whether a capability is required until then is an anti-pattern, so use of this
     * method is strongly discouraged.</strong> It only exists to avoid the need to break backward compatibility by removing
     * support for expressions from certain attributes.
     * </p>
     *
     * @param required the name of the required capability. Cannot be {@code null}
     * @param dependent the name of the capability that requires the other capability. Cannot be {@code null}
     * @param attribute the name of the attribute that triggered this requirement, or {@code null} if no single
     *                  attribute was responsible
     *
     * @throws OperationClientException if the requested capability is not available
     */
    void requireOptionalCapability(String required, String dependent, String attribute) throws OperationClientException;

    /**
     * Gets the runtime API associated with a given capability, if there is one.
     * @param capabilityName the name of the capability. Cannot be {@code null}
     * @param apiType class of the java type that exposes the API. Cannot be {@code null}
     * @param <T> the java type that exposes the API
     * @return the runtime API. Will not return {@code null}
     *
     * @throws java.lang.IllegalArgumentException if the capability does not provide a runtime API
     * @throws java.lang.ClassCastException if the runtime API exposed by the capability cannot be cast to type {code T}
     */
    <T> T getCapabilityRuntimeAPI(String capabilityName, Class<T> apiType);

    /**
     * Gets the runtime API associated with a given {@link RuntimeCapability#isDynamicallyNamed() dynamically named}
     * capability, if there is one.
     *
     * @param capabilityBaseName the base name of the capability. Cannot be {@code null}
     * @param dynamicPart the dynamic part of the capability name. Cannot be {@code null}
     * @param apiType class of the java type that exposes the API. Cannot be {@code null}
     * @param <T> the java type that exposes the API
     * @return the runtime API. Will not return {@code null}
     *
     * @throws java.lang.IllegalArgumentException if the capability does not provide a runtime API
     * @throws java.lang.ClassCastException if the runtime API exposed by the capability cannot be cast to type {code T}
     */
    <T> T getCapabilityRuntimeAPI(String capabilityBaseName, String dynamicPart, Class<T> apiType);

    /**
     * Gets a support object that allows service implementations installed from this context to
     * integrate with capabilities.
     *
     * @return the support object. Will not return {@code null}
     */
    CapabilityServiceSupport getCapabilityServiceSupport();

    /**
     * Handler for a callback to a {@link RuntimeUpdateHandler} indicating that the result of the overall operation is
     * known and the handler can take any necessary actions to deal with that result.
     */
    @FunctionalInterface
    interface ResultHandler {

        /**
         * Callback to an {@link RuntimeUpdateHandler} indicating that the result of the overall operation is
         * known and the handler can take any necessary actions to deal with that result.
         *
         * @param resultAction the overall result of the operation
         * @param context  the operation execution context; will be the same as what was passed to the
         *                 {@link RuntimeUpdateHandler#execute(RuntimeUpdateContext)} method invocation
         *                 that registered this rollback handler.
         */
        void handleResult(RuntimeOperationContext.ResultAction resultAction, RuntimeUpdateContext context);
    }

    /**
     * Handler for a callback to an {@link RuntimeUpdateHandler} indicating that the overall operation is being
     * rolled back and the handler should revert any change it has made. This is simply a helper implementation
     * of {@link org.wildfly.management.api.runtime.RuntimeReadContext.ResultHandler} that ignores the
     * {@link org.wildfly.management.api.runtime.RuntimeOperationContext.ResultAction#KEEP} case.
     */
    @FunctionalInterface
    interface RollbackHandler {

        /**
         * A {@link RuntimeUpdateContext.RollbackHandler} that calls {@link RuntimeUpdateContext#revertReloadRequired()}. Intended for use by
         * operation step handlers call {@link RuntimeUpdateContext#reloadRequired()} and perform no other actions
         * that need to be rolled back.
         */
        RuntimeUpdateContext.RollbackHandler REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER = new RuntimeUpdateContext.RollbackHandler() {
            /**
             * Does nothing.
             *
             * @param context  ignored
             */
            @Override
            public void handleRollback(RuntimeUpdateContext context) {
                context.revertReloadRequired();
            }
        };


        /**
         * Callback to an {@link RuntimeUpdateHandler} indicating that the overall operation is being rolled back and the
         * handler should revert any change it has made. A handler need not remove services
         * installed by the operation; this will be done automatically.
         *
         * @param context  the operation execution context; will be the same as what was passed to the
         *                 {@link RuntimeUpdateHandler#execute(RuntimeUpdateContext)} method invocation
         *                 that registered this rollback handler.
         */
        void handleRollback(RuntimeUpdateContext context);
    }

}
