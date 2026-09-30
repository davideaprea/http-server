package io.github.davideaprea.httpserver.client;

import io.github.davideaprea.httpserver.reader.dto.Context;
import io.github.davideaprea.httpserver.reader.lifecycle.RequestLineReader;
import io.github.davideaprea.httpserver.reader.lifecycle.RequestReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Reads request data from a client socket and passes it to the request reader
 * lifecycle.
 */
public class ClientInputChannel {
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;

    private RequestReader requestReader;

    public ClientInputChannel(Context context) {
        socketChannel = context.channelKey().getSocketChannel();
        buffer = ByteBuffer.allocateDirect(8192);
        requestReader = new RequestLineReader(context);
    }

    /**
     * Reads available data from the client socket and processes it as part of an
     * HTTP request.
     *
     * <p>Reading stops when no more data is currently available, the request
     * reader signals that it cannot proceed, or the client connection is closed.</p>
     */
    public void read() {
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
                requestReader.close();
            }

            if (bytesRead <= 0) break;
        }
    }
}
