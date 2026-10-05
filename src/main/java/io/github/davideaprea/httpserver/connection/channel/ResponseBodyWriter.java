package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.Response;

import java.util.function.Consumer;

/**
 * Writes an HTTP response body into a queue of buffers that are later
 * transmitted to the client channel.
 *
 * <p>Implementations define how the response body is encoded before being
 * queued for transmission.</p>
 */
public abstract class ResponseBodyWriter {
    protected final Consumer<byte[]> onBodyChunk;

    protected ResponseBodyWriter(Consumer<byte[]> onBodyChunk) {
        this.onBodyChunk = onBodyChunk;
    }

    public abstract void write(Response response) throws Exception;
}
