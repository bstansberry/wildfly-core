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

package org.wildfly.management.api._private;


import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Provides internationalized log messages and exceptions for the classes in the extension API.
 *
 * @author Brian Stansberry
 */
@MessageLogger(projectCode = "WFLYMGT", length = 4)
public interface ManagementApiLogger extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    ManagementApiLogger ROOT_LOGGER = Logger.getMessageLogger(ManagementApiLogger.class, "org.wildfly.management");
    
}
