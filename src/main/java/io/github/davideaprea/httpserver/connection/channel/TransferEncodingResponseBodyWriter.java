package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.Response;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class TransferEncodingResponseBodyWriter extends ResponseBodyWriter {
    public TransferEncodingResponseBodyWriter(BlockingQueue<ByteBuffer> outgoingBodyChunks) {
        super(outgoingBodyChunks);
    }

    @Override
    public void write(Response response) throws Exception {
        try (InputStream bodyStream = response.body()) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = bodyStream.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                outgoingBodyChunks.put(ByteBuffer.wrap((Integer.toHexString(bytesRead) + "\r\n").getBytes()));
                outgoingBodyChunks.put(ByteBuffer.wrap(Arrays.copyOf(buffer, bytesRead)));
                outgoingBodyChunks.put(ByteBuffer.wrap("\r\n".getBytes()));
            }

            outgoingBodyChunks.put(ByteBuffer.wrap("0\r\n\r\n".getBytes()));
        }
    }
}
