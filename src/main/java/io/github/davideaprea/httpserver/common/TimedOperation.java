package io.github.davideaprea.httpserver.common;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules an action to be executed after a specified delay and allows the
 * scheduled operation to be cancelled.
 */
@RequiredArgsConstructor
public class TimedOperation {
    private final ScheduledExecutorService scheduler;
    private final long timeout;
    private final TimeUnit unit;
    private final Runnable action;

    private ScheduledFuture<?> future;

    /**
     * Starts the timed operation.
     *
     * <p>If an operation is already scheduled, it is cancelled before scheduling
     * a new one.</p>
     */
    public synchronized void start() {
        stop();

        future = scheduler.schedule(
                action,
                timeout,
                unit
        );
    }

    /**
     * Stops the scheduled operation, if any.
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);

            future = null;
        }
    }
}
