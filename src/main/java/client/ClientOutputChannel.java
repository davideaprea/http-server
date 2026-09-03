package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientOutputChannel extends ClientChannel {
    private final BlockingQueue<ByteBuffer> bodyChunks = new LinkedBlockingQueue<>();

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

            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (IOException e) {
            close();
        }
    }

    public void write(byte[] bodyChunk) {
        try {
            bodyChunks.put(ByteBuffer.wrap(bodyChunk));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientKey.selector().wakeup();
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
    }
}
