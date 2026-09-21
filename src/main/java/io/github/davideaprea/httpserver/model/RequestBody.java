package io.github.davideaprea.httpserver.model;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Represents the body of an HTTP request as a bounded, thread-safe buffer
 * of bytes.
 * <p>The buffer allows bytes to be produced and consumed concurrently while
 * enforcing a maximum capacity. A callback can be used to notify the producer
 * when space becomes available in a full buffer.</p>
 */
public class RequestBody {
    private static final int MAX = 8192;

    private final Queue<Integer> bufferedBytes = new ArrayDeque<>();
    private final Runnable onSpaceFreed;

    private boolean isClosed = false;

    /**
     * @param onSpaceFreed the function that will be called when the buffer is free
     *                     and available to receive new data
     */
    public RequestBody(Runnable onSpaceFreed) {
        this.onSpaceFreed = onSpaceFreed;
    }

    /**
     * Adds a byte to the request body.
     *
     * @param bodyByte the byte to add
     * @throws IllegalStateException if the buffer is full or the request body is closed
     */
    public synchronized void enqueue(int bodyByte) {
        if (isClosed || bufferedBytes.size() == MAX) {
            throw new IllegalStateException();
        }

        boolean wasFull = bufferedBytes.size() == MAX - 1;

        bufferedBytes.add(bodyByte);

        if (wasFull) {
            notifyAll();
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
     * no bytes remain
     * @throws IllegalStateException if the thread is interrupted while waiting
     *                               for a byte
     */
    public synchronized int dequeue() {
        while (bufferedBytes.isEmpty() && !isClosed) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(e);
            }
        }

        if (bufferedBytes.isEmpty()) {
            return -1;
        }

        int bodyByte = bufferedBytes.remove();

        if (bufferedBytes.size() == MAX - 1) {
            onSpaceFreed.run();
        }

        notifyAll();

        return bodyByte;
    }

    public synchronized boolean isFull() {
        return bufferedBytes.size() == MAX;
    }

    /**
     * Closes the request body.
     *
     * <p>Once closed, no more bytes can be added to the request body. Bytes
     * already buffered can still be consumed.</p>
     */
    public synchronized void close() {
        isClosed = true;
        notifyAll();
    }
}
