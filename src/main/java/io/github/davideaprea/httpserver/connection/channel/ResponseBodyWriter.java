package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.Response;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public abstract class ResponseBodyWriter {
    protected final BlockingQueue<ByteBuffer> outgoingBodyChunks;

    public ResponseBodyWriter(BlockingQueue<ByteBuffer> outgoingBodyChunks) {
        this.outgoingBodyChunks = outgoingBodyChunks;
    }

    public abstract void write(Response response) throws Exception;
}
