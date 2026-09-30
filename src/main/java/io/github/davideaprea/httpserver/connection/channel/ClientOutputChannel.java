package io.github.davideaprea.httpserver.connection.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages outgoing HTTP response data for a channel.
 *
 * <p>Response data is queued by the handler thread through {@link #write(byte[], boolean)}
 * and written to the socket by the selector thread through {@link #flush()}.</p>
 */
public class ClientOutputChannel {
    private static final int MAX = 16;

    private final BlockingQueue<ByteBuffer> bodyChunks = new LinkedBlockingQueue<>(MAX);
    private final ClientChannelKey clientChannelKey;

    private ByteBuffer lastChunkBeforeClosingConnection;

    public ClientOutputChannel(ClientChannelKey clientChannelKey) {
        this.clientChannelKey = clientChannelKey;
    }

    /**
     * Writes queued response data to the socket.
     *
     * <p>Writing stops when the socket cannot accept more data. Once all queued
     * data has been written, write interest is removed from the selector.</p>
     */
    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                ByteBuffer buffer = bodyChunks.peek();
                int written = clientChannelKey.getSocketChannel().write(buffer);

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();

                    if (buffer == lastChunkBeforeClosingConnection) {
                        clientChannelKey.close();
                        lastChunkBeforeClosingConnection = null;

                        return;
                    }
                }

                if (written == 0) {
                    return;
                }
            }

            clientChannelKey.removeWriteInterest();
        } catch (IOException e) {
            clientChannelKey.close();
        }
    }

    /**
     * Queues a response chunk to be written to the socket.
     *
     * <p>The chunk is queued for writing by the selector thread.</p>
     *
     * @param chunk  the response data to queue
     * @param isLast whether the chunk is the last one to be written before
     *               closing the connection
     */
    public void write(byte[] chunk, boolean isLast) {
        try {
            ByteBuffer bodyChunk = ByteBuffer.wrap(chunk);

            bodyChunks.put(bodyChunk);

            if (isLast) {
                lastChunkBeforeClosingConnection = bodyChunk;
            }
        } catch (InterruptedException e) {
            clientChannelKey.close();

            throw new RuntimeException(e);
        }

        clientChannelKey.addWriteInterest();
    }
}
