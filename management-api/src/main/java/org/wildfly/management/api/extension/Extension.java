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

package org.wildfly.management.api.extension;

/**
 * An extension to a WildFly Core based managed process.  Implementations of this interface must
 * have a zero-arg constructor.  Extension modules must contain a {@code META-INF/services/org.wildfly.management.api.extension.Extension}
 * file with a line containing the name of the implementation class.
 *
 * @author Brian Stansberry
 */
public interface Extension {

    /**
     * Initialize this extension by registering its operation handlers and configuration
     * marshaller with the given {@link ExtensionContext}.
     * <p>When this method is invoked the {@link Thread#getContextClassLoader() thread context classloader} will
     * be set to be the defining class loader of the class that implements this interface.</p>
     *
     * @param context the extension context
     */
    void initialize(ExtensionContext context);

    /**
     * Gets whether this extension is only usable in a domain wide configuration, with attempted use
     * on a server or for a particular host controller resulting in an exception.
     *
     * @return {@code true} if this extension is only usable in a domain wide configuration
     */
    boolean isDomainOnly();
}
