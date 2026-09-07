package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientOutputChannel {
    private final BlockingQueue<ByteBuffer> bodyChunks = new LinkedBlockingQueue<>();
    private final ClientChannelKey clientChannelKey;

    public ClientOutputChannel(ClientChannelKey clientChannelKey) {
        this.clientChannelKey = clientChannelKey;
    }

    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                ByteBuffer buffer = bodyChunks.peek();
                int written = clientChannelKey.getSocketChannel().write(buffer);

                if (written == 0) {
                    return;
                }

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();
                }
            }

            clientChannelKey.removeWriteInterest();
        } catch (IOException e) {
            clientChannelKey.close();
        }
    }

    public void write(ByteBuffer bodyChunk) {
        try {
            bodyChunks.put(bodyChunk);
        } catch (InterruptedException e) {
            clientChannelKey.close();

            throw new RuntimeException(e);
        }

        clientChannelKey.addWriteInterest();
    }
}
