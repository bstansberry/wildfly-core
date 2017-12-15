/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_ENVIRONMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.as.host.controller.discovery.DiscoveryOptionsResource;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.logging.HostControllerLogger;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.platform.mbean.RootPlatformMBeanResource;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleClassLoader;

public final class InitialHostDefinition extends SimpleResourceDefinition {

    static final OperationContext.AttachmentKey<Boolean> HOST_ADD_AFTER_BOOT = OperationContext.AttachmentKey.create(Boolean.class);

    private final ManagementResourceRegistration root;
    private final HostControllerEnvironment environment;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;
    private final HostModelUtil.HostModelRegistrar hostModelRegistrar;
    private final Resource modelControllerResource;
    private final LocalHostControllerInfo localHostControllerInfo;

    public InitialHostDefinition(
            final ManagementResourceRegistration root,
            final HostControllerEnvironment environment,
            final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
            final HostModelUtil.HostModelRegistrar hostModelRegistrar,
            final Resource modelControllerResource,
            final LocalHostControllerInfoImpl localHostControllerInfo) {
        super(new Parameters(PathElement.pathElement(HOST), HostModelUtil.getResourceDescriptionResolver()));
        this.root = root;
        this.environment = environment;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
        this.hostModelRegistrar = hostModelRegistrar;
        this.modelControllerResource = modelControllerResource;
        this.localHostControllerInfo = localHostControllerInfo;
    }

    private void registerHostModel(final String hostName) {
        hostModelRegistrar.registerHostModel(hostName, root);
    }

    private void initCoreModel(final ModelNode model) {
        initCoreModel(model, environment);
    }

    private void initModelServices(final OperationContext context, final PathAddress hostAddress, final Resource rootResource) {
        // Create the management resources
        Resource management = context.createResource(hostAddress.append(PathElement.pathElement(CORE_SERVICE, MANAGEMENT)));
        if (modelControllerResource != null) {
            management.registerChild(PathElement.pathElement(SERVICE, MANAGEMENT_OPERATIONS), modelControllerResource);
        }

        //Create the empty host-environment resource
        context.addResource(hostAddress.append(PathElement.pathElement(CORE_SERVICE, HOST_ENVIRONMENT)), PlaceholderResource.INSTANCE);

        //Create the empty module-loading resource
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MODULE_LOADING), PlaceholderResource.INSTANCE);

        //Create the empty capability registry resource
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.CAPABILITY_REGISTRY), PlaceholderResource.INSTANCE);

        // Wire in the platform mbean resources. We're bypassing the context.createResource API here because
        // we want to use our own resource type. But it's ok as the createResource calls above have taken the lock
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());
        // Wire in the ignored-resources resource
        Resource.ResourceEntry ignoredRoot = ignoredDomainResourceRegistry.getRootResource();
        rootResource.registerChild(ignoredRoot.getPathElement(), ignoredRoot);

        // Create the empty discovery options resource
        context.addResource(hostAddress.append(PathElement.pathElement(CORE_SERVICE, DISCOVERY_OPTIONS)), new DiscoveryOptionsResource());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration hostDefinition) {
        super.registerOperations(hostDefinition);
        hostDefinition.registerOperationHandler(HostAddHandler.DEFINITION, new HostAddHandler(this));
    }

    private static void initCoreModel(final ModelNode root, HostControllerEnvironment environment) {

        try {
            root.get(RELEASE_VERSION).set(Version.AS_VERSION);
            root.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        } catch (RuntimeException e) {
            if (HostAddHandler.class.getClassLoader() instanceof ModuleClassLoader) {
                //The standalone tests can't get this info
                throw e;
            }
        }
        root.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        root.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);
        root.get(MANAGEMENT_MICRO_VERSION).set(Version.MANAGEMENT_MICRO_VERSION);

        // Community uses UNDEF values
        ModelNode nameNode = root.get(PRODUCT_NAME);
        ModelNode versionNode = root.get(PRODUCT_VERSION);

        if (environment != null) {
            String productName = environment.getProductConfig().getProductName();
            String productVersion = environment.getProductConfig().getProductVersion();

            if (productName != null) {
                nameNode.set(productName);
            }
            if (productVersion != null) {
                versionNode.set(productVersion);
            }
        }

        //Set empty lists for namespaces and schema-locations to pass model validation
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
    }

    /**
     * The handler to add the local host definition to the DomainModel.
     *
     * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
     * @author <a href="mailto:kwills@redhat.com">Ken Wills</a>
     */
    private static class HostAddHandler extends AbstractAddStepHandler {

        private static final OperationContext.AttachmentKey<String> HOST_NAME = OperationContext.AttachmentKey.create(String.class);

        private static final RuntimeCapability<Void> HOST_RUNTIME_CAPABILITY = RuntimeCapability
                .Builder.of("org.wildfly.host.controller", false)
                .build();

        private static final SimpleAttributeDefinition PERSIST_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PERSIST_NAME, ModelType.BOOLEAN)
                .setRequired(false)
                .setDefaultValue(new ModelNode().set(false))
                .build();

        private static final SimpleAttributeDefinition IS_DOMAIN_CONTROLLER = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.IS_DOMAIN_CONTROLLER, ModelType.BOOLEAN)
                .setRequired(false)
                .setDefaultValue(new ModelNode().set(Boolean.TRUE))
                .setDeprecated(ModelVersion.create(6), false)
                .build();

        private static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(ADD, HostResolver.getResolver(HOST))
                .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
                .addParameter(PERSIST_NAME)
                .addParameter(IS_DOMAIN_CONTROLLER)
                .build();

        private final InitialHostDefinition hostDefinition;

        private HostAddHandler(final InitialHostDefinition hostDefinition) {
            this.hostDefinition = hostDefinition;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final PathAddress pa = context.getCurrentAddress();
            // if we're not already at the root, call this at the root as an immediate step
            if (!pa.equals(PathAddress.EMPTY_ADDRESS)) {
                final ModelNode cloned = operation.clone();
                cloned.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
                context.attach(HOST_NAME, pa.getLastElement().getValue());
                context.addStep(cloned, this, OperationContext.Stage.MODEL, true);
                return;
            }

            // see if a host has already been added.
            Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
            if (root.getChildrenNames(HOST).size() > 0) {
                // there is a host already registered
                final String exists = root.getChildrenNames(HOST).iterator().next();
                throw HostControllerLogger.ROOT_LOGGER.cannotAddHostAlreadyRegistered(exists);
            }

            final String hostName = context.getAttachment(HOST_NAME);
            if (hostName == null) {
                throw HostControllerLogger.ROOT_LOGGER.nullHostName();
            }

            boolean persistName = false;
            if (operation.has(ModelDescriptionConstants.PERSIST_NAME)) {
                persistName = operation.get(ModelDescriptionConstants.PERSIST_NAME).asBoolean();
            }

            boolean isDomainController = true;
            if (operation.has(ModelDescriptionConstants.IS_DOMAIN_CONTROLLER)) {
                isDomainController = operation.get(ModelDescriptionConstants.IS_DOMAIN_CONTROLLER).asBoolean();
            }

            final ModelNode dc = new ModelNode();
            if (isDomainController) {
                dc.get(LOCAL).setEmptyObject();
            } else {
                dc.get(REMOTE).setEmptyObject();
            }

            if (!context.isBooting() && !isDomainController) {
                // this is a slave add using /host=foo:add() manually. Don't allow this.
                throw HostControllerLogger.ROOT_LOGGER.cannotAddSlaveHostAfterBoot();
            }

            context.registerCapability(HOST_RUNTIME_CAPABILITY);

            final PathAddress hostAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, hostName));
            final LocalHostControllerInfo localHostControllerInfo = hostDefinition.localHostControllerInfo;
            // register as DC or slave, we do this before registering the host definition.
            ((LocalHostControllerInfoImpl) localHostControllerInfo).setMasterDomainController(isDomainController || !context.isBooting());

            hostDefinition.registerHostModel(hostName);
            final Resource rootResource = context.createResource(hostAddress);
            final ModelNode model = rootResource.getModel();
            model.get(DOMAIN_CONTROLLER).set(dc);
            hostDefinition.initCoreModel(model);

            // check to see if we need to enable domainController to allow host op routing
            // this is only used during an empty config boot, the parsers will add the necessary op
            // for a normal boot.
            if (isDomainController || !context.isBooting()) {
                final ManagementResourceRegistration rootRegistration = context.getResourceRegistrationForUpdate();
                final ModelNode update = new ModelNode();
                update.get(OP_ADDR).set(hostAddress.toModelNode());
                update.get(OP).set(LocalDomainControllerAddHandler.OPERATION_NAME);
                context.attach(HOST_ADD_AFTER_BOOT, !context.isBooting());
                context.addStep(update, rootRegistration.getOperationHandler(hostAddress, LocalDomainControllerAddHandler.OPERATION_NAME), OperationContext.Stage.MODEL, true);
            }
            hostDefinition.initModelServices(context, hostAddress, rootResource);

            // if we added with /host=foo:add(persist-name=true) write-attribute on the hc name
            if (!context.isBooting() && persistName) {
                final ManagementResourceRegistration rootRegistration = context.getResourceRegistrationForUpdate();
                final ModelNode name = new ModelNode();
                name.get(OP_ADDR).set(hostAddress.toModelNode());
                name.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                name.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.NAME);
                name.get(ModelDescriptionConstants.VALUE).set(hostName);
                context.addStep(name, rootRegistration.getOperationHandler(hostAddress, WRITE_ATTRIBUTE_OPERATION), OperationContext.Stage.MODEL, false);
            }
        }

    }
}
