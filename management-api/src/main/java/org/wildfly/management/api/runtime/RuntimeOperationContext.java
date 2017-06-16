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

import java.io.InputStream;
import java.util.logging.Level;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.management.api.PathAddress;
import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.ProcessType;
import org.wildfly.management.api.RunningMode;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.ResourceType;
import org.wildfly.management.api.model.Resource;
import org.wildfly.management.api.notification.Notification;

/**
 * Base interface for providers of contextual information and operations made available to a runtime management
 * operation handler as it executes.
 *
 * @author Brian Stansberry
 */
public interface RuntimeOperationContext {

    /**
     * Add a warning to the operation response. This method appends warning message in response headers.
     * Warnings should be issued to inform the client invoking the operation of non catastrophic occurrences,
     * which may require administrative action.
     *
     * @param level - level of warning. Used to filter warning based on level value, just like
     * @param warning - i18n formatter message, it should contain ID, just like jboss.Logger output does.
     */
    void addResponseWarning(Level level, String warning);

    /**
     * See {@link #addResponseWarning(Level, String)}
     * @param warning - pre-formatted warning messsage.
     */
    void addResponseWarning(Level level, ModelNode warning);

    /**
     * Get a stream which is attached to the request.
     *
     * @param index the index
     * @return the input stream
     */
    InputStream getAttachmentStream(int index);

    /**
     * Gets the number of streams attached to the request.
     *
     * @return  the number of streams
     */
    int getAttachmentStreamCount();

    /**
     * Get the node into which the operation result should be written.
     *
     * @return the result node
     */
    ModelNode getResult();

    /**
     * Returns whether {@link #getResult()} has been invoked.
     *
     * @return {@code true} if {@link #getResult()} has been invoked
     */
    boolean hasResult();

    /**
     * Attach a stream to be included as part of the response. The return value of this method should be
     * used as the value of the {@link #getResult() result} for the step that invokes this method. Callers
     * can then use that value to find the stream in the
     * {@code org.jboss.as.controller.client.OperationResponse} to this operation.
     *
     * @param mimeType the mime type of the stream. Cannot be {@code null}
     * @param stream the stream. Cannot be {@code null}
     * @return a uuid for the stream. Will not be {@code null}
     *
     * @throws IllegalStateException if {@link #isBooting()} returns {@code true}.
     */
    String attachResultStream(String mimeType, InputStream stream);

    /**
     * Attach a stream to be included as part of the response, with a predetermined UUID.
     * <p>
     * This method is intended for use by core handlers related to managed domain operation
     * as they propagate a stream throughout a domain. Ordinary handlers should use
     * {@link #attachResultStream(String, java.io.InputStream)}.
     *
     * @param mimeType the mime type of the stream. Cannot be {@code null}
     * @param stream the stream. Cannot be {@code null}
     *
     * @throws IllegalStateException if {@link #isBooting()} returns {@code true}.
     */
    void attachResultStream(String uuid, String mimeType, InputStream stream);

    /**
     * Get the failure description response node, creating it if necessary.
     *
     * @return the failure description
     */
    ModelNode getFailureDescription();

    /**
     * Returns whether {@link #getFailureDescription()} has been invoked.
     *
     * @return {@code true} if {@link #getFailureDescription()} has been invoked
     */
    boolean hasFailureDescription();

    /**
     * Get the type of process in which this operation is executing.
     *
     * @return the process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the running mode of the process.
     *
     * @return   the running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Determine whether the process is currently performing boot tasks.
     *
     * @return whether the process is currently booting
     */
    boolean isBooting();

    /**
     * Convenience method to check if the {@link #getProcessType() process type} is {@link ProcessType#isServer() a server type}
     * and the {@link #getRunningMode() running mode} is {@link RunningMode#NORMAL}. The typical usage would
     * be for handlers that are only meant to execute on a normally running server, not on a host controller
     * or on a {@link RunningMode#ADMIN_ONLY} server.
     *
     * @return {@code true} if the {@link #getProcessType() process type} is {@link ProcessType#isServer() a server type}
     *         and the {@link #getRunningMode() running mode} is {@link RunningMode#NORMAL}.
     */
    boolean isNormalServer();

    /**
     * Determine whether the current operation is bound to be rolled back.
     *
     * @return {@code true} if the operation will be rolled back
     */
    boolean isRollbackOnly();

    /**
     * Gets whether the overall operation is configured to rollback if any if an error is introduced
     * by a {@link RuntimeOperationContext.Stage#RUNTIME} or {@link RuntimeOperationContext.Stage#VERIFY} handler.
     *
     * @return {@code true} if the operation should rollback if there is a runtime stage failure
     */
    boolean isRollbackOnRuntimeFailure();

    /**
     * Gets the address associated with the currently executing step.
     * @return the address. Will not be {@code null}
     */
    PathAddress getCurrentAddress();

    /**
     * Gets the {@link PathElement#getValue() value} of the {@link #getCurrentAddress() current address'}
     * {@link PathAddress#getLastElement() last element}.
     *
     * @return the last element value
     *
     * @throws java.lang.IllegalStateException if {@link #getCurrentAddress()} is the empty address
     */
    String getCurrentAddressValue();

    /**
     * Gets the name of the operation associated with the currently executing operation step.
     *
     * @return the name. Will not be {@code null}
     */
    String getCurrentOperationName();

    /**
     * Gets the value of the parameter of the given name for the operation associated with the currently executing operation step.
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @return the parameter value. Will not return {@code null}, returning of a node of {@link org.jboss.dmr.ModelType#UNDEFINED} if there was no such parameter
     */
    ModelNode getCurrentOperationParameter(String parameterName);

    /**
     * Gets whether the operation associated with the currently executing operation step
     * {@link ModelNode#has(String) has} a parameter with the given name.
     *
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @return {@code true} if the currently executing operation has a parameter with the given name.
     */
    boolean hasCurrentOperationParameter(String parameterName);

    /**
     * Get a read only view of the managed resource registration.  The registration is relative to the operation address.
     *
     * @return the model node registration
     */
    ResourceType getResourceRegistration();

    /**
     * Get a read only view of the root managed resource registration.
     *
     * @return the root resource registration
     */
    ResourceType getRootResourceRegistration();

    /**
     * Get the resource for read only operations, relative to the executed operation address. Reads never block.
     * If a write action was previously performed, the value read will be from an uncommitted copy of the the management model.<br/>
     *
     * Note: By default the returned resource is read-only copy of the entire sub-model. In case this is not required use
     * {@link RuntimeOperationContext#readResource(PathAddress, boolean)} instead.
     *
     * @param relativeAddress the (possibly empty) address where the resource should be added. The address is relative to the
     *                address of the operation being executed
     * @return the resource
     */
    Resource readResource(PathAddress relativeAddress);

    /**
     * Get the resource for read only operations, relative to the executed operation address. Reads never block.
     * If a write action was previously performed, the value read will be from an uncommitted copy of the the management model.
     *
     * @param relativeAddress the (possibly empty) address where the resource should be added. The address is relative to the
     *                address of the operation being executed
     * @param recursive whether the model should be read recursively or not
     * @return the resource
     */
    Resource readResource(PathAddress relativeAddress, boolean recursive);

    /**
     * Read an addressable resource from the root of the model. Reads never block. If a write action was previously performed,
     * the value read will be from an uncommitted copy of the the management model.
     * <p>
     * Note: By default the returned resource is read-only copy of the entire sub-model. In case the entire sub-model
     * is not required use {@link RuntimeOperationContext#readResourceFromRoot(PathAddress, boolean)} instead.
     *
     * @param address the (possibly empty) address
     * @return a read-only reference from the model
     */
    Resource readResourceFromRoot(PathAddress address);

    /**
     * Read an addressable resource from the root of the model. Reads never block. If a write action was previously performed,
     * the value read will be from an uncommitted copy of the the management model.
     * <p>
     * Use the {@code recursive} parameter to avoid the expense of making read-only copies of large portions of the
     * resource tree. If {@code recursive} is {@code false}, the returned resource will only have placeholder resources
     * for immediate children. Those placeholder resources will return an empty
     * {@link Resource#getModel() model} and will not themselves have any children.
     * Their presence, however, allows the caller to see what immediate children exist under the target resource.
     *
     * @param address the (possibly empty) address
     * @param recursive whether the model should be read recursively or not
     * @return a read-only reference from the model
     */
    Resource readResourceFromRoot(PathAddress address, boolean recursive);

    /**
     * Get a read-only reference of the entire management model BEFORE any changes were made by this context.
     * The structure of the returned model may depend on the context type (domain vs. server).
     *
     * @return the read-only original resource
     */
    Resource getOriginalRootResource();

    /**
     * Get the current stage of execution.
     *
     * @return the current stage
     */
    Stage getCurrentStage();

    /**
     * Retrieves an object that has been attached to this context.
     *
     * @param key the key to the attachment.
     * @param <T> the value type of the attachment.
     *
     * @return the attachment if found otherwise {@code null}.
     */
    <T> T getAttachment(AttachmentKey<T> key);

    /**
     * Attaches an arbitrary object to this context.
     *
     * @param key   they attachment key used to ensure uniqueness and used for retrieval of the value.
     * @param value the value to store.
     * @param <T>   the value type of the attachment.
     *
     * @return the previous value associated with the key or {@code null} if there was no previous value.
     */
    <T> T attach(AttachmentKey<T> key, T value);

    /**
     * Attaches an arbitrary object to this context only if the object was not already attached. If a value has already
     * been attached with the key provided, the current value associated with the key is returned.
     *
     * @param key   they attachment key used to ensure uniqueness and used for retrieval of the value.
     * @param value the value to store.
     * @param <T>   the value type of the attachment.
     *
     * @return the previous value associated with the key or {@code null} if there was no previous value.
     */
    <T> T attachIfAbsent(AttachmentKey<T> key, T value);

    /**
     * Detaches or removes the value from this context.
     *
     * @param key the key to the attachment.
     * @param <T> the value type of the attachment.
     *
     * @return the attachment if found otherwise {@code null}.
     */
    <T> T detach(AttachmentKey<T> key);

    /**
     * Emit a {@link Notification}.
     *
     * @param notification the notification to emit
     */
    void emit(final Notification notification);

    /**
     * Checks whether one of a capability's optional and runtime-only requirements is present. Only for use in cases
     * where the {@code dependent} capability's persistent configuration does not <strong>mandate</strong> the presence
     * of the {@code requested} capability, but the capability will use it at runtime if it is present.
     * <p>
     * This method should be used when the caller's own configuration doesn't impose a hard requirement for the
     * {@code requested} capability, but, if it is present it will be used. Once the caller declares an intent to use
     * the capability by invoking this method and getting a {@code true} response, thereafter the system is aware that
     * {@code dependent} is actually using {@code requested}, but <strong>will not</strong> prevent configuration
     * changes that make {@code requested} unavailable.
     * </p>
     *
     * @param requested the name of the requested capability. Cannot be {@code null}
     * @param dependent the name of the capability that requires the other capability. Cannot be {@code null}
     * @param attribute the name of the attribute that triggered this requirement, or {@code null} if no single
     *                  attribute was responsible
     * @return {@code true} if the requested capability is present; {@code false} if not. If {@code true}, hereafter
     *         {@code dependent}'s requirement for {@code requested} will not be treated as optional.
     */
    boolean hasOptionalCapability(String requested, String dependent, String attribute);

    /**
     * Gets the name of a service associated with a given capability, if there is one.
     * @param capabilityName the name of the capability. Cannot be {@code null}
     * @param serviceType class of the java type that exposes by the service. Cannot be {@code null}
     * @return the name of the service. Will not return {@code null}
     *
     * @throws IllegalArgumentException if {@code serviceType} is {@code null } or
     *            the capability does not provide a service of type {@code serviceType}
     */
    ServiceName getCapabilityServiceName(String capabilityName, Class<?> serviceType);

    /**
     * Gets the name of a service associated with a given {@link RuntimeCapability#isDynamicallyNamed() dynamically named}
     * capability, if there is one.
     *
     * @param capabilityBaseName the base name of the capability. Cannot be {@code null}
     * @param dynamicPart the dynamic part of the capability name. Cannot be {@code null}
     * @param serviceType class of the java type that exposes by the service. Cannot be {@code null}
     * @return the name of the service. Will not return {@code null}
     *
     * @throws IllegalArgumentException if {@code serviceType} is {@code null } or
     *            the capability does not provide a service of type {@code serviceType}
     */
    ServiceName getCapabilityServiceName(String capabilityBaseName, String dynamicPart, Class<?> serviceType);


    /**
     * Gets the name of a service associated with a given {@link RuntimeCapability#isDynamicallyNamed() dynamically named}
     * capability, if there is one.
     *
     * @param capabilityBaseName the base name of the capability. Cannot be {@code null}
     * @param serviceType class of the java type that exposes by the service. Cannot be {@code null}
     * @param dynamicParts the dynamic parts of the capability name. Cannot be {@code null}
     * @return the name of the service. Will not return {@code null}
     *
     * @throws IllegalArgumentException if {@code serviceType} is {@code null } or
     *            the capability does not provide a service of type {@code serviceType}
     */
    ServiceName getCapabilityServiceName(String capabilityBaseName, Class<?> serviceType, String ... dynamicParts);

    /**
     * The stage at which a step should apply.
     */
    enum Stage {
        /**
         * The step applies to the runtime container (read or write).
         */
        RUNTIME,
        /**
         * The step checks the result of a runtime container operation (read only).  Inspect the container,
         * and if problems are detected, record the problem(s) in the operation result.
         */
        VERIFY,
        /**
         * The operation has completed execution.
         */
        DONE;

        Stage() {
        }

        boolean hasNext() {
            return this != DONE;
        }

        RuntimeUpdateContext.Stage next() {
            switch (this) {
                case RUNTIME: return VERIFY;
                case VERIFY: return DONE;
                case DONE:
                default: throw new IllegalStateException();
            }
        }
    }

    /**
     * The result action.
     */
    enum ResultAction {
        /**
         * The operation will be committed to the model and/or runtime.
         */
        KEEP,
        /**
         * The operation will be reverted.
         */
        ROLLBACK,
    }

    /**
     * An attachment key instance.
     *
     * @param <T> the attachment value type
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final class AttachmentKey<T> {
        private final Class<T> valueClass;

        /**
         * Construct a new instance.
         *
         * @param valueClass the value type.
         */
        private AttachmentKey(final Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Cast the value to the type of this attachment key.
         *
         * @param value the value
         *
         * @return the cast value
         */
        public T cast(final Object value) {
            return valueClass.cast(value);
        }

        /**
         * Construct a new simple attachment key.
         *
         * @param valueClass the value class
         * @param <T>        the attachment type
         *
         * @return the new instance
         */
        @SuppressWarnings("unchecked")
        public static <T> AttachmentKey<T> create(final Class<? super T> valueClass) {
            return new AttachmentKey(valueClass);
        }
    }
}
