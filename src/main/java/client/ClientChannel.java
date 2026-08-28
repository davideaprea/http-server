package client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class ClientChannel {
    protected final SelectionKey clientKey;

    protected ClientChannel(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }


    protected void close() {
        clientKey.cancel();

        try {
            ((SocketChannel) clientKey.channel()).close();
        } catch (IOException e) {
            System.out.println("Error while closing socket channel: " + e.getMessage());
        }
    }
}
