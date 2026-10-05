package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.connection.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.connection.lifecycle.RequestLineReader;
import io.github.davideaprea.httpserver.connection.lifecycle.RequestReader;
import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Response;
import io.github.davideaprea.httpserver.router.Router;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Represents a client connection and manages its HTTP request and response
 * processing lifecycle.
 *
 * <p>The channel is responsible for reading requests from the client socket,
 * processing queued responses, buffering response data, and writing that data
 * back to the socket through the selector.</p>
 */
public class ClientChannel {
    private final SelectionKey selectionKey;
    private final ByteBuffer buffer;
    @Getter
    private final TimedOperation requestTimer;
    @Getter
    private final SizeLimits sizeLimits;
    @Getter
    private final Router router;
    private final BlockingQueue<ByteBuffer> bodyChunks;
    private final ExecutorService executorService;
    private final Queue<EnqueuedResponse> responsesQueue;

    private Future<?> ongoingResponseWriting;
    private ByteBuffer lastChunkBeforeClosingConnection;
    private RequestReader requestReader;

    public ClientChannel(SelectionKey selectionKey, TimedOperation requestTimer, SizeLimits sizeLimits, Router router, ExecutorService executorService) {
        Objects.requireNonNull(selectionKey);
        Objects.requireNonNull(requestTimer);
        Objects.requireNonNull(sizeLimits);
        Objects.requireNonNull(router);
        Objects.requireNonNull(executorService);

        this.selectionKey = selectionKey;
        this.requestTimer = requestTimer;
        this.sizeLimits = sizeLimits;
        this.router = router;
        this.executorService = executorService;

        buffer = ByteBuffer.allocateDirect(8192);
        bodyChunks = new LinkedBlockingQueue<>(16);
        responsesQueue = new LinkedList<>();
        requestReader = new RequestLineReader(this);
    }

    /**
     * Adds a response supplier to the processing queue.
     *
     * <p>If no response is currently being processed, processing starts
     * immediately.</p>
     */
    public synchronized void enqueue(EnqueuedResponse responseSupplier) {
        if (!selectionKey.channel().isOpen()) {
            return;
        }

        responsesQueue.add(responseSupplier);

        if (ongoingResponseWriting == null) {
            submitNext();
        }
    }

    private synchronized void submitNext() {
        if (!selectionKey.channel().isOpen()) {
            return;
        }

        ongoingResponseWriting = null;
        EnqueuedResponse enqueuedResponse = responsesQueue.poll();

        if (enqueuedResponse == null) {
            return;
        }

        ongoingResponseWriting = executorService.submit(() -> {
            Response response = enqueuedResponse.responseSupplier().get();

            try {
                bodyChunks.put(ByteBuffer.wrap(response.toHTTPFrame().getBytes()));

                enableInterest(SelectionKey.OP_WRITE);

                if (!enqueuedResponse.shouldSkipBodyProcessing()) {
                    Consumer<byte[]> bodyChunksConsumer = bytes -> {
                        try {
                            bodyChunks.put(ByteBuffer.wrap(bytes));

                            enableInterest(SelectionKey.OP_WRITE);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    ResponseBodyWriter responseBodyWriter;

                    if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                        responseBodyWriter = new ContentLengthResponseBodyWriter(bodyChunksConsumer);
                    } else {
                        responseBodyWriter = new TransferEncodingResponseBodyWriter(bodyChunksConsumer);
                    }

                    responseBodyWriter.write(response);
                }

                if (response.headers().getOrDefault(HeaderKey.CONNECTION.getValue(), "").equals("close")) {
                    lastChunkBeforeClosingConnection = ByteBuffer.wrap(new byte[0]);

                    bodyChunks.put(lastChunkBeforeClosingConnection);
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();

                close();
            }

            if (!Thread.currentThread().isInterrupted()) {
                submitNext();
            }
        });
    }

    /**
     * Reads available data from the socket and processes it as part of an
     * HTTP request.
     *
     * <p>Reading stops when no more data is currently available, the request
     * reader signals that it cannot proceed, or the channel is closed.</p>
     */
    public void read() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (!socketChannel.isOpen()) {
            return;
        }

        while (true) {
            buffer.flip();

            while (buffer.hasRemaining() && requestReader.isFree()) {
                requestReader = requestReader.evaluate(buffer.get());
            }

            buffer.compact();

            int bytesRead;

            try {
                bytesRead = socketChannel.read(buffer);
            } catch (IOException e) {
                bytesRead = -1;
            }

            if (bytesRead == -1) {
                close();
            }

            if (bytesRead <= 0) break;
        }
    }

    /**
     * Writes queued response data to the socket.
     *
     * <p>Writing stops when the socket cannot accept more data. Once all queued
     * data has been written, write interest is removed from the selector.</p>
     */
    public void flush() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (!socketChannel.isOpen()) {
            return;
        }

        try {
            while (!bodyChunks.isEmpty()) {
                ByteBuffer buffer = bodyChunks.peek();
                int written = socketChannel.write(buffer);

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();

                    if (buffer == lastChunkBeforeClosingConnection) {
                        close();
                        lastChunkBeforeClosingConnection = null;

                        return;
                    }
                }

                if (written == 0) {
                    return;
                }
            }

            disableInterest(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            close();
        }
    }

    /**
     * Disables the specified selection interest operation for this channel.
     *
     * @param interest the selection key operation to disable.
     * {@link SelectionKey} values ar meant to be used
     */
    public void disableInterest(int interest) {
        selectionKey.interestOps(selectionKey.interestOps() & ~interest);
        selectionKey.selector().wakeup();
    }

    /**
     * Enables the specified selection interest operation for this channel.
     *
     * @param interest the selection key operation to enable.
     * {@link SelectionKey} values ar meant to be used
     */
    public void enableInterest(int interest) {
        selectionKey.interestOps(selectionKey.interestOps() | interest);
        selectionKey.selector().wakeup();
    }

    /**
     * Closes the client connection and releases all resources associated with it.
     *
     * <p>Pending response processing is cancelled, queued responses and buffered
     * response data are discarded, and the selection key and underlying channel
     * are closed.</p>
     */
    public void close() {
        requestTimer.stop();

        if (ongoingResponseWriting != null) {
            ongoingResponseWriting.cancel(true);

            ongoingResponseWriting = null;
        }

        responsesQueue.clear();
        selectionKey.cancel();
        bodyChunks.clear();

        try {
            selectionKey.channel().close();
        } catch (IOException e) {
            System.out.println("Error while closing socket connection: " + e.getMessage());
        }
    }
}
