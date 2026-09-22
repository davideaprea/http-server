package io.github.davideaprea.httpserver.client;

import io.github.davideaprea.httpserver.client.dto.OutputChunk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages outgoing HTTP response data for a client connection.
 *
 * <p>Response data is queued by the handler thread through {@link #write(byte[], boolean)}
 * and written to the client socket by the selector thread through {@link #flush()}.</p>
 */
public class ClientOutputChannel {
    private static final int MAX = 16;

    private final BlockingQueue<OutputChunk> bodyChunks = new LinkedBlockingQueue<>(MAX);
    private final ClientChannelKey clientChannelKey;

    public ClientOutputChannel(ClientChannelKey clientChannelKey) {
        this.clientChannelKey = clientChannelKey;
    }

    /**
     * Writes queued response data to the client socket.
     *
     * <p>Writing stops when the socket cannot accept more data. Once all queued
     * data has been written, write interest is removed from the selector.</p>
     */
    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                OutputChunk chunk = bodyChunks.peek();
                ByteBuffer buffer = chunk.value();
                int written = clientChannelKey.getSocketChannel().write(buffer);

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();

                    if (chunk.isLast()) {
                        clientChannelKey.close();

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
     * Queues a response chunk to be written to the client socket.
     *
     * <p>The chunk is queued for writing by the selector thread.</p>
     *
     * @param chunk the response data to queue
     * @param isLast whether the chunk is the last one to be written before
     *               closing the channel
     */
    public void write(byte[] chunk, boolean isLast) {
        try {
            bodyChunks.put(new OutputChunk(ByteBuffer.wrap(chunk), isLast));
        } catch (InterruptedException e) {
            clientChannelKey.close();

            throw new RuntimeException(e);
        }

        clientChannelKey.addWriteInterest();
    }
}
