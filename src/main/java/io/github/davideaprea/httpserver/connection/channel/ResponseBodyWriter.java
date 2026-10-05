package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.Response;

import java.util.function.Consumer;

public abstract class ResponseBodyWriter {
    protected final Consumer<byte[]> onBodyChunk;

    protected ResponseBodyWriter(Consumer<byte[]> onBodyChunk) {
        this.onBodyChunk = onBodyChunk;
    }

    public abstract void write(Response response) throws Exception;
}
