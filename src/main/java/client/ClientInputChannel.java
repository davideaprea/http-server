package client;

import common.queue.RequestQueue;
import reader.RequestLineReader;
import reader.RequestReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientInputChannel {
    private final SelectionKey clientKey;
    private final ByteBuffer buffer;

    private RequestReader requestReader;

    public ClientInputChannel(SelectionKey clientKey, RequestQueue requestQueue) {
        this.clientKey = clientKey;
        buffer = ByteBuffer.allocateDirect(8192);
        requestReader = new RequestLineReader(requestQueue);
    }

    public void read() {
        SocketChannel client = (SocketChannel) clientKey.channel();
        int bytesRead;

        while (true) {
            try {
                if (!((bytesRead = client.read(buffer)) > 0)) break;
            } catch (IOException e) {
                System.out.println("Error while reading from client socket: " + e.getMessage());

                close();

                return;
            }

            buffer.flip();

            while (buffer.hasRemaining()) {
                requestReader = requestReader.eval(buffer.get());
            }

            buffer.clear();
        }

        if (bytesRead == -1) {
            close();
        }
    }

    private void close() {
        clientKey.cancel();

        try {
            ((SocketChannel) clientKey.channel()).close();
        } catch (IOException e) {
            System.out.println("Error while closing socket channel: " + e.getMessage());
        }
    }
}
