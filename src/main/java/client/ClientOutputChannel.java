package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientOutputChannel {
    private final BlockingQueue<OutputChunk> bodyChunks = new LinkedBlockingQueue<>();
    private final ClientChannelKey clientChannelKey;

    public ClientOutputChannel(ClientChannelKey clientChannelKey) {
        this.clientChannelKey = clientChannelKey;
    }

    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                OutputChunk chunk = bodyChunks.peek();
                ByteBuffer buffer = chunk.value();
                int written = clientChannelKey.getSocketChannel().write(buffer);

                if (written == 0) {
                    return;
                }

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();
                }

                if (chunk.isLast()) {
                    clientChannelKey.close();

                    return;
                }
            }

            clientChannelKey.removeWriteInterest();
        } catch (IOException e) {
            clientChannelKey.close();
        }
    }

    public void write(byte[] chunk, boolean isLast) {
        try {
            bodyChunks.put(new OutputChunk(ByteBuffer.wrap(chunk), isLast));
        } catch (InterruptedException e) {
            clientChannelKey.close();

            throw new RuntimeException(e);
        }

        clientChannelKey.addWriteInterest();
    }
}
