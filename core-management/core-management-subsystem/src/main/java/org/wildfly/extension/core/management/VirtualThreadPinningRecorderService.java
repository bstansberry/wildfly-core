/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.core.management;

import java.time.Duration;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

final class VirtualThreadPinningRecorderService implements Service {

    static void install(CapabilityServiceTarget target, Mode mode, Logger.Level logLevel, int stackTraceDepth) {
        if (mode != Mode.NEVER) {

        }
    }

    private final Logger.Level logLevel;
    private final int stackTraceDepth;
    private volatile RecordingStream recordingStream;

    VirtualThreadPinningRecorderService(Logger.Level logLevel, int stackTraceDepth) {
        this.logLevel = logLevel;
        this.stackTraceDepth = stackTraceDepth;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        RecordingStream rs = new RecordingStream();
        rs.enable("jdk.VirtualThreadPinned").withStackTrace();
        rs.onEvent("jdk.VirtualThreadPinned", VirtualThreadPinningRecorderService::onEvent);
        rs.setMaxAge(Duration.ofSeconds(10));

        try {
            rs.startAsync();
            recordingStream = rs;
        } catch (RuntimeException e) {
            rs.close();
            throw e;
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        if (recordingStream != null) {
            recordingStream.close();
        }
    }

    private static void onEvent(RecordedEvent event) {
        // todo
    }

    enum Mode {
        NEVER,
        ON_DEMAND,
        ALWAYS
    }
}
