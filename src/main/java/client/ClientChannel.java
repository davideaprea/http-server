package client;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class ClientChannel {
    protected final SelectionKey clientKey;

    protected ClientChannel(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }


    public void close() {
        clientKey.cancel();

        try {
            clientKey.channel().close();
        } catch (IOException e) {
            System.out.println("Error while closing socket channel: " + e.getMessage());
        }
    }
}
