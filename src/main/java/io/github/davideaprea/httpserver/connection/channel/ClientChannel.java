package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.connection.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.connection.lifecycle.RequestLineReader;
import io.github.davideaprea.httpserver.connection.lifecycle.RequestReader;
import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Response;
import io.github.davideaprea.httpserver.router.Router;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@Builder
public class ClientChannel {
    private final SelectionKey selectionKey;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
    @Getter
    private final TimedOperation requestTimer;
    @Getter
    private final SizeLimits sizeLimits;
    @Getter
    private final Router router;
    private final BlockingQueue<ByteBuffer> bodyChunks = new LinkedBlockingQueue<>(16);
    private final ExecutorService executorService;
    private final Queue<EnqueuedResponse> responsesQueue = new LinkedList<>();

    private Future<?> ongoingResponseWriting;
    private ByteBuffer lastChunkBeforeClosingConnection;
    private RequestReader requestReader = new RequestLineReader(this);

    public ClientChannel(SelectionKey selectionKey, TimedOperation requestTimer, SizeLimits sizeLimits, Router router, ExecutorService executorService) {
        this.selectionKey = selectionKey;
        this.requestTimer = requestTimer;
        this.sizeLimits = sizeLimits;
        this.router = router;
        this.executorService = executorService;
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
            enableInterest(SelectionKey.OP_WRITE);

            Response response = enqueuedResponse.responseSupplier().get();

            try {
                bodyChunks.put(ByteBuffer.wrap(response.toHTTPFrame().getBytes()));

                if (!enqueuedResponse.shouldSkipBodyProcessing()) {
                    ResponseBodyWriter responseBodyWriter;

                    if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                        responseBodyWriter = new ContentLengthResponseBodyWriter(bodyChunks);
                    } else {
                        responseBodyWriter = new TransferEncodingResponseBodyWriter(bodyChunks);
                    }

                    responseBodyWriter.write(response);
                }

                if (response.headers().getOrDefault(HeaderKey.CONNECTION.getValue(), "").equals("close")) {
                    lastChunkBeforeClosingConnection = ByteBuffer.wrap(new byte[0]);

                    bodyChunks.put(lastChunkBeforeClosingConnection);
                }
            } catch (Exception e) {
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

    public void disableInterest(int interest) {
        selectionKey.interestOps(selectionKey.interestOps() & ~interest);
        selectionKey.selector().wakeup();
    }

    public void enableInterest(int interest) {
        selectionKey.interestOps(selectionKey.interestOps() | interest);
        selectionKey.selector().wakeup();
    }

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
