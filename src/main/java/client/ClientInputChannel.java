package client;

import common.queue.RequestQueue;
import reader.RequestLineReader;
import reader.RequestReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientInputChannel {
    private final SocketChannel client;
    private final ByteBuffer buffer;

    private RequestReader requestReader;

    public ClientInputChannel(SocketChannel client, RequestQueue requestQueue) {
        this.client = client;
        buffer = ByteBuffer.allocateDirect(8192);
        requestReader = new RequestLineReader(requestQueue);
    }

    public void read() throws IOException {
        int bytesRead;

        while ((bytesRead = client.read(buffer)) > 0) {
            buffer.flip();

            while (buffer.hasRemaining()) {
                requestReader = requestReader.eval(buffer.get());
            }

            buffer.clear();
        }
    }
}
