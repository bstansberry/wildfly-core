/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.server;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tracks elapsed time since either JVM start or a moment of initialization. The start point
 * for the calculation can be 'reset', allowing the calculation provided by an instance to
 * account for a conceptual 'restart'.
 */
public final class ElapsedTime {

    /**
     * Creates a tracker that tracks elapsed time since JVM start.
     * @return the tracker. Will not return {@code null}.
     */
    public static ElapsedTime startingFromJvmStart() {
        return new ElapsedTime(null);
    }

    /**
     * Creates a tracker that tracks elapsed time since the invocation of this method.
     * @return the tracker. Will not return {@code null}.
     */
    public static ElapsedTime startingFromNow() {
        return new ElapsedTime(System.currentTimeMillis());
    }

    private volatile Long startTime;

    // Guarded by this
    private final Map<ElapsedTime, Object > checkpoints = new WeakHashMap<>();

    private ElapsedTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the tracker's start point.
     *
     * @return the start point for this tracker, in milliseconds since the epoch.
     */
    public long getStartTime() {
        return startTime != null ? startTime : ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    /**
     * Get the elapsed time in milliseconds since this tracker's start point.
     *
     * @return the elapsed time
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - getStartTime();
    }

    /**
     * Reset this tracker to begin tracking from the {@link System#currentTimeMillis()} current time}.
     * Any ElapsedTime objects that were returned from this object's {@link #checkpoint()} method will also be reset.
     * Meant for cases where the 'origin' moment may have changed and this tracker should be updated accordingly --
     * for example, in a 'restored' JVM that supports some form of checkpoint and restore behavior.
     */
    public synchronized void reset() {
        // following a CRIU restore, at least on OpenJ9, the VM start time and uptime
        // from RuntimeMXBean don't seem to reflect when the restore happened.
        // So all we can do is reset to the current time and ignore the primordial VM start/restore time before we are called
        reset(System.currentTimeMillis());
    }

    private synchronized void reset(Long startTime) {
        this.startTime = startTime;
        for (ElapsedTime checkpoint : checkpoints.keySet()) {
            checkpoint.reset(startTime);
        }

    }

    /**
     * Create an ElapsedTime that tracks elapsed time from the current time, but
     * whose starting point will automatically be {@link #reset() reset} if this object is reset.
     */
    public synchronized ElapsedTime checkpoint() {
        ElapsedTime checkpoint = ElapsedTime.startingFromNow();
        checkpoints.put(checkpoint, null);
        return checkpoint;
    }


}
