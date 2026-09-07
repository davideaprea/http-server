package model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestBody {
    private static final int MAX = 8192;

    private final BlockingQueue<Integer> bufferedBytes = new LinkedBlockingQueue<>(MAX);
    private final Runnable onSpaceFreed;
    private final AtomicBoolean isFull = new AtomicBoolean(false);

    public RequestBody(Runnable onSpaceFreed) {
        this.onSpaceFreed = onSpaceFreed;
    }

    public void enqueue(int bodyByte) {
        if (isFull.get()) {
            throw new IllegalStateException();
        }

        bufferedBytes.add(bodyByte);

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
            throw new IllegalStateException(e);
        }
    }

    public boolean isFull() {
        return isFull.get();
    }
}
