package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ClientOutputChannel implements Consumer<byte[]> {
    private final SelectionKey clientKey;
    private final SocketChannel socketChannel;
    private final Queue<ByteBuffer> bodyChunks = new ConcurrentLinkedQueue<>();

    public ClientOutputChannel(SelectionKey clientKey, SocketChannel socketChannel) {
        this.clientKey = clientKey;
        this.socketChannel = socketChannel;
    }

    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                ByteBuffer buffer = bodyChunks.peek();
                int written = socketChannel.write(buffer);

                if (written == 0) {
                    return;
                }

                if (!buffer.hasRemaining()) {
                    bodyChunks.poll();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    @Override
    public void accept(byte[] bodyChunk) {
        bodyChunks.add(ByteBuffer.wrap(bodyChunk));
        clientKey.selector().wakeup();
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
    }
}
