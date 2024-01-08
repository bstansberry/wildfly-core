/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.embedded;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossExecutors;

final class CaptureFuture<T> extends AsyncFutureTask<T> {

    CaptureFuture() {
        super(JBossExecutors.directExecutor());
    }

    void set(T result) {
        setResult(result);
    }



    /**
     * Install a service that captures the value provided by a given target and stores it in the given AsyncFuture.
     * <p>
     * TODO investigate using ServiceValueExecutorRegistry for this. An issue would be the added dependency.
     *
     * @param ctx {$link ServiceActivatorContext} to use to install the service. Cannot be {@code null}.
     * @param target the name of the service whose value should be captured. Cannot be {@code null}.
     * @param future  store for the captured value. Cannot be {@code null}.
     * @param <T> type of the value to capture
     */
    static <T> void captureServiceValue(ServiceActivatorContext ctx, ServiceName target, CaptureFuture<T> future) {
        ServiceBuilder<?> sb = ctx.getServiceTarget().addService();
        final Supplier<T> targetSupplier = sb.requires(target);
        sb.setInstance(new Service() {
            @Override
            public void start(StartContext context) {
                future.set(targetSupplier.get());
                context.getController().setMode(ServiceController.Mode.REMOVE);
            }

            @Override
            public void stop(StopContext context) {
            }
        });
        sb.install();
    }
}
