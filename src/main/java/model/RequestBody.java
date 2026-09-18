package model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents the body of an HTTP request as a bounded, thread-safe buffer
 * of bytes.
 * <p>The buffer allows bytes to be produced and consumed concurrently while
 * enforcing a maximum capacity. A callback can be used to notify the producer
 * when space becomes available in a full buffer.</p> */
public class RequestBody {
    private static final int MAX = 8192;

    private final BlockingQueue<Integer> bufferedBytes = new LinkedBlockingQueue<>(MAX);
    private final Runnable onSpaceFreed;
    private final AtomicBoolean isFull = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * @param onSpaceFreed the function that will be called when the buffer is free
     *                     and available to receive new data
     */
    public RequestBody(Runnable onSpaceFreed) {
        this.onSpaceFreed = onSpaceFreed;
    }

    /**
     * Adds a byte to the request body.
     * @param bodyByte the byte to add
     * @throws IllegalStateException if the buffer is full or the request body is closed */
    public void enqueue(int bodyByte) {
        if (isFull.get() || isClosed.get()) {
            throw new IllegalStateException();
        }

        bufferedBytes.add(bodyByte);

        if (bufferedBytes.size() == MAX) {
            isFull.set(true);
        }
    }

    /**
     * Retrieves and removes the next byte from the request body.
     *
     * <p>If no byte is available, this method blocks until one becomes available
     * or the request body is closed. If the request body is closed and no bytes
     * remain, {@code -1} is returned.</p>
     *
     * @return the next byte, or {@code -1} if the request body is closed and
     *         no bytes remain
     * @throws IllegalStateException if the thread is interrupted while waiting
     *                               for a byte
     */
    public int dequeue() {
        if (isClosed.get() && bufferedBytes.isEmpty()) {
            return -1;
        }

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

    /**
     * Closes the request body.
     *
     * <p>Once closed, no more bytes can be added to the request body. Bytes
     * already buffered can still be consumed.</p>
     */
    public void close() {
        isClosed.set(true);
    }
}
