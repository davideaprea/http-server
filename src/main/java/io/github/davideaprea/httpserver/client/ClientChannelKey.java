package io.github.davideaprea.httpserver.client;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ClientChannelKey {
    private final SelectionKey clientKey;
    private final List<Runnable> onCloseSubscribers = new ArrayList<>();

    public void subscribeToCloseEvent(Runnable runnable) {
        onCloseSubscribers.add(runnable);
    }

    public void removeReadInterest() {
        clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
        clientKey.selector().wakeup();
    }

    public void addReadInterest() {
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_READ);
        clientKey.selector().wakeup();
    }

    public void removeWriteInterest() {
        clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_WRITE);
        clientKey.selector().wakeup();
    }

    public void addWriteInterest() {
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
        clientKey.selector().wakeup();
    }

    public void close() {
        clientKey.cancel();

        try {
            clientKey.channel().close();
        } catch (IOException e) {
            System.out.println("Error while closing socket channel: " + e.getMessage());
        }

        onCloseSubscribers.forEach(Runnable::run);
    }

    public SocketChannel getSocketChannel() {
        return (SocketChannel) clientKey.channel();
    }
}
