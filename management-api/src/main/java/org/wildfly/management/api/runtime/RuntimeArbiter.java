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

import org.wildfly.management.api.PathAddress;
import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.ProcessType;
import org.wildfly.management.api.RunningMode;

/**
 * Decides whether an operation against a particular resource requires any runtime handling.
 * Implementations of this interface must be public and must provide a public no argument constructor.
 *
 * @author Brian Stansberry
 *
 * @deprecated Currently unused; will be removed unless used
 */
@Deprecated
public interface RuntimeArbiter {

    boolean isRuntimeUpdateRequired(Context context);

    interface Context {

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
         * Whether normally this operation would require a runtime step. It returns {@code true in the following cases}
         * <ul>
         *  <li>The process is a server, and it is running in NORMAL (i.e. not admin-only) mode.</li>
         *  <li>The process is a HC, and the address of the operation is a subsystem in the host model or a child thereof</li>
         */
        boolean isDefaultRequiresRuntime();

    }
}
