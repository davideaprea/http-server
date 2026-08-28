package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientOutputChannel extends ClientChannel {
    private final Queue<ByteBuffer> bodyChunks = new ConcurrentLinkedQueue<>();

    public ClientOutputChannel(SelectionKey clientKey) {
        super(clientKey);
    }

    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                ByteBuffer buffer = bodyChunks.peek();
                int written = ((SocketChannel) clientKey.channel()).write(buffer);

                if (written == 0) {
                    return;
                }

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();
                }
            }
        } catch (IOException e) {
            close();
        }
    }

    public void write(byte[] bodyChunk) {
        bodyChunks.add(ByteBuffer.wrap(bodyChunk));
        clientKey.selector().wakeup();
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
    }
}
