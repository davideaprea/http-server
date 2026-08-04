package writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class ResponseWriter implements Consumer<byte[]> {
    private final SocketChannel socketChannel;
    private final Queue<ByteBuffer> bodyChunks = new LinkedList<>();
    private final Consumer<ResponseWriter> newBodyChunkEventConsumer;

    public ResponseWriter(SocketChannel socketChannel, Consumer<ResponseWriter> newBodyChunkEventConsumer) {
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
        newBodyChunkEventConsumer.accept(this);
    }
}
