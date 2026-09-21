package io.github.davideaprea.httpserver.client;

import io.github.davideaprea.httpserver.reader.lifecycle.RequestReaderEvaluator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Reads request data from a client socket and passes it to the request reader
 * lifecycle.
 */
public class ClientInputChannel {
    private final ClientChannelKey clientChannelKey;
    private final ByteBuffer buffer;
    private final RequestReaderEvaluator requestReaderEvaluator;

    public ClientInputChannel(ClientChannelKey clientChannelKey, RequestReaderEvaluator requestReaderEvaluator) {
        this.clientChannelKey = clientChannelKey;
        this.requestReaderEvaluator = requestReaderEvaluator;
        buffer = ByteBuffer.allocateDirect(8192);
    }

    /**
     * Reads available data from the client socket and processes it as part of an
     * HTTP request.
     *
     * <p>Reading stops when no more data is currently available, the request
     * reader signals that it cannot proceed, or the client connection is closed.</p>
     */
    public void read() {
        boolean isFree = true;
        SocketChannel client = clientChannelKey.getSocketChannel();
        int bytesRead;

        while (true) {
            try {
                bytesRead = client.read(buffer);
            } catch (IOException e) {
                bytesRead = -1;
            }

            if (bytesRead <= 0) break;

            buffer.flip();

            while (buffer.hasRemaining() && isFree) {
                isFree = requestReaderEvaluator.eval(buffer.get());
            }

            buffer.compact();
        }

        if (!isFree) {
            clientChannelKey.removeReadInterest();
        }

        if (bytesRead == -1) {
            clientChannelKey.close();
        }
    }
}
