package server;

import client.Client;
import client.ClientInputChannel;
import common.queue.RequestQueue;
import client.ClientOutputChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ServerConfiguration configuration;
    private final ExecutorService executor;

    private Selector selector;

    public Server(ServerConfiguration configuration) {
        this.configuration = configuration;
        executor = Executors.newFixedThreadPool(configuration.threadPoolSize());
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(configuration.port()));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (selector.isOpen() && serverChannel.isOpen()) {
            selector.select();

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    SocketChannel client = ((ServerSocketChannel) key.channel()).accept();

                    client.configureBlocking(false);

                    SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
                    ClientOutputChannel outputChannel = new ClientOutputChannel(clientKey);

                    clientKey.attach(new Client(
                            new ClientInputChannel(clientKey, new RequestQueue(configuration.router(), executor, outputChannel)),
                            outputChannel
                    ));
                } else if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();

                    ((Client) key.attachment()).inputChannel().read();
                } else if (key.isWritable()) {
                    ((Client) key.attachment()).outputChannel().flush();
                }
            }
        }
    }

    public void stop() throws IOException {
        selector.close();
        executor.shutdownNow();
    }
}
