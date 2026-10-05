package io.github.davideaprea.httpserver.connection.channel;

import io.github.davideaprea.httpserver.model.Response;

import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public class TransferEncodingResponseBodyWriter extends ResponseBodyWriter {
    protected TransferEncodingResponseBodyWriter(Consumer<byte[]> onBodyChunk) {
        super(onBodyChunk);
    }

    /**
     * Writes the response body using HTTP chunked transfer encoding.
     *
     * <p>The body is divided into chunks, each preceded by its hexadecimal size
     * and followed by a CRLF sequence. A zero-length chunk is written after the
     * body to signal the end of the transfer.</p>
     *
     * @param response the HTTP response to write
     * @throws Exception if the response body cannot be read or written
     */
    @Override
    public void write(Response response) throws Exception {
        try (InputStream bodyStream = response.body()) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = bodyStream.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                onBodyChunk.accept((Integer.toHexString(bytesRead) + "\r\n").getBytes());
                onBodyChunk.accept(Arrays.copyOf(buffer, bytesRead));
                onBodyChunk.accept("\r\n".getBytes());
            }

            onBodyChunk.accept("0\r\n\r\n".getBytes());
        }
    }
}
