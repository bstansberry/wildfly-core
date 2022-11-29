/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.host.controller.maven;

import static org.jboss.as.controller.AbstractControllerService.PATH_MANAGER_CAPABILITY;
import static org.jboss.as.server.mgmt.UndertowHttpManagementService.EXTENSIBLE_HTTP_MANAGEMENT_CAPABILITY;

import java.nio.file.Path;
import java.util.function.Supplier;

import io.undertow.server.handlers.resource.PathResourceManager;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.host.controller.HostControllerService;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Creates a 'maven-proxy' context on the HTTP management interface that serves files from a configured path.
 * Intent is that path would be the root of a filesystem-based maven repository.
 */
public final class MavenProxySupport {

    private static final ServiceName SERVICE_NAME = HostControllerService.HC_SERVICE_NAME.append("maven-proxy");

    public static void install(ServiceTarget serviceTarget) {
        // TODO configuration via system property is just a POC thing although it may survive
        String path = WildFlySecurityManager.getPropertyPrivileged("unsupported.org.jboss.domain.maven-proxy.path", null);
        if (path != null && !path.isEmpty()) {
            String relativeTo = WildFlySecurityManager.getPropertyPrivileged("unsupported.org.jboss.domain.maven-proxy.relative-to", null);
            ServiceBuilder<?> builder = serviceTarget.addService(SERVICE_NAME);
            Supplier<ExtensibleHttpManagement> extensibleHttpManagement = builder.requires(EXTENSIBLE_HTTP_MANAGEMENT_CAPABILITY.getCapabilityServiceName());
            Supplier<PathManager> pathManagerSupplier = builder.requires(PATH_MANAGER_CAPABILITY.getCapabilityServiceName());
            builder.setInstance(new MavenProxyService(extensibleHttpManagement, pathManagerSupplier, path, relativeTo));
            builder.install();
        }
    }

    private static class MavenProxyService implements Service {

        private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
        private final Supplier<PathManager> pathManagerSupplier;
        private final String path;
        private final String relativeTo;
        private volatile PathManager.Callback.Handle relativeToCallbackHandle;

        private MavenProxyService(final Supplier<ExtensibleHttpManagement> extensibleHttpManagement,
                                  final Supplier<PathManager> pathManagerSupplier,
                                  final String path,
                                  final String relativeTo) {
            this.extensibleHttpManagement = extensibleHttpManagement;
            this.pathManagerSupplier = pathManagerSupplier;
            this.path = path;
            this.relativeTo = relativeTo;
        }

        @Override
        public void start(StartContext context) throws StartException {
            PathManager pathManager = pathManagerSupplier.get();
            Path nioPath = Path.of(pathManager.resolveRelativePathEntry(path, relativeTo));
            if (relativeTo != null) {
                relativeToCallbackHandle = pathManager.registerCallback(relativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.REMOVED, PathManager.Event.UPDATED);
            }
            extensibleHttpManagement.get().addStaticContext("maven-proxy",
                    PathResourceManager.builder().setBase(nioPath).build());
        }

        @Override
        public void stop(StopContext context) {
            extensibleHttpManagement.get().removeContext("maven-proxy");
            if (relativeToCallbackHandle != null) {
                relativeToCallbackHandle.remove();
            }
        }
    }
}
