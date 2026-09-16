package client;

import reader.lifecycle.RequestReaderEvaluator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientInputChannel {
    private final ClientChannelKey clientChannelKey;
    private final ByteBuffer buffer;
    private final RequestReaderEvaluator requestReaderEvaluator;

    private boolean isFree;

    public ClientInputChannel(ClientChannelKey clientChannelKey, RequestReaderEvaluator requestReaderEvaluator) {
        this.clientChannelKey = clientChannelKey;
        this.requestReaderEvaluator = requestReaderEvaluator;
        buffer = ByteBuffer.allocateDirect(8192);
        isFree = true;
    }

    public void read() {
        if (!isFree) {
            return;
        }

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
