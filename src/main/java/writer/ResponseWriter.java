package writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class ResponseWriter implements Consumer<byte[]> {
    private final OutputStream socketOutputStream;
    private final Queue<byte[]> bodyChunks = new LinkedList<>();

    public ResponseWriter(OutputStream socketOutputStream) {
        this.socketOutputStream = socketOutputStream;
    }

    public void flush() {
        try {
            while (!bodyChunks.isEmpty()) {
                socketOutputStream.write(bodyChunks.poll());
                socketOutputStream.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    @Override
    public void accept(byte[] bodyChunk) {
        bodyChunks.add(bodyChunk);
    }
}
