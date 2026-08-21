package writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ResponseWriter implements Consumer<byte[]> {
    private final SocketChannel socketChannel;
    private final Queue<ByteBuffer> bodyChunks = new ConcurrentLinkedQueue<>();
    private final Runnable newBodyChunkEventConsumer;

    public ResponseWriter(SocketChannel socketChannel, Runnable newBodyChunkEventConsumer) {
        this.socketChannel = socketChannel;
        this.newBodyChunkEventConsumer = newBodyChunkEventConsumer;
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
        newBodyChunkEventConsumer.run();
    }
}
