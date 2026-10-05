package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Response;

import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public class ContentLengthResponseBodyWriter extends ResponseBodyWriter {
    protected ContentLengthResponseBodyWriter(Consumer<byte[]> onBodyChunk) {
        super(onBodyChunk);
    }

    public void write(Response response) throws Exception {
        try (InputStream bodyStream = response.body()) {
            long bytesToWrite = Long.parseLong(response.headers().get(HeaderKey.CONTENT_LENGTH.getValue()));
            byte[] buffer = new byte[8192];

            while (bytesToWrite > 0) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                int maxRead = (int) Math.min(buffer.length, bytesToWrite);
                int bytesRead = bodyStream.read(buffer, 0, maxRead);

                if (bytesRead == -1) {
                    throw new IllegalStateException();
                }

                onBodyChunk.accept(Arrays.copyOf(buffer, bytesRead));

                bytesToWrite -= bytesRead;
            }
        }
    }
}
