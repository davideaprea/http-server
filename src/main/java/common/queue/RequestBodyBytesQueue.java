package common.queue;

import common.exception.ResponseStatusException;
import model.Status;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestBodyBytesQueue {
    private static final int MAX = 8192;

    private final BlockingQueue<Integer> bufferedBytes = new LinkedBlockingQueue<>(MAX);
    private final Runnable onSpaceFreed;
    private final AtomicBoolean isFull = new AtomicBoolean(false);

    public RequestBodyBytesQueue(Runnable onSpaceFreed) {
        this.onSpaceFreed = onSpaceFreed;
    }

    public void enqueue(int bodyByte) {
        bufferedBytes.offer(bodyByte);

        if (bufferedBytes.size() == MAX) {
            isFull.set(true);
        }
    }

    public int dequeue() {
        try {
            int bodyByte = bufferedBytes.take();

            if (isFull.get()) {
                isFull.set(false);

                onSpaceFreed.run();
            }

            return bodyByte;
        } catch (InterruptedException e) {
            throw new ResponseStatusException(Status.REQUEST_TIMEOUT);
        }
    }

    public boolean isFull() {
        return isFull.get();
    }
}
