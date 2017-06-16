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

package org.wildfly.management.api;

/**
 * Functionality levels at which a managed process can be configured to run.
 *
 * @author Brian Stansberry
 */
public enum RunLevel {

//    /**
//     * No managed resources provide runtime services. Only useful when the process is embedded, as without
//     * runtime services from managed resources external administration is not possible. This level is
//     * really only meant for internal testing uses.
//     */
//    MODEL_ONLY,
    /**
     * The only managed resources that provide runtime services are those necessary for a management client
     * connecting internally from within a process in which the managed process is embedded. Management resources that
     * provide remote management capabilities do not install runtime services. The managed process is not visible on
     * the network.
     */
    EMBEDDED_ADMIN_ONLY,
    /**
     * The only managed resources that provide runtime services are those that may be related to managing the process.
     * Resources that provide remote management start their runtime services, so the process is visible on the network.
     */
    ADMIN_ONLY,
    /**
     * All managed resources install their runtime services, but those that support a 'suspended' mode of operation
     * are suspended.
     */
    SUSPENDED,
    /** Full operation. */
    NORMAL
}
