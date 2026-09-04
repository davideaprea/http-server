package common;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TimedOperation {
    private final ScheduledExecutorService scheduler;
    private final long timeout;
    private final TimeUnit unit;
    private final Runnable action;

    private ScheduledFuture<?> future;

    public synchronized void start() {
        stop();

        future = scheduler.schedule(
                action,
                timeout,
                unit
        );
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);

            future = null;
        }
    }
}
