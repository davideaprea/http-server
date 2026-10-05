package io.github.davideaprea.httpserver.server;

import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.connection.channel.ClientChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs an HTTP server that accepts socket connections and processes HTTP
 * requests using a non-blocking I/O model.
 */
public class Server {
    private final Configuration configuration;
    private final ExecutorService executor;
    private final ScheduledExecutorService timersScheduler;

    private Selector selector;

    public Server(Configuration configuration) {
        this.configuration = configuration;
        executor = Executors.newFixedThreadPool(configuration.threadPoolSize());
        timersScheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Starts the server and processes socket connections and I/O events.
     *
     * <p>This method blocks while the server is running.</p>
     */
    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(configuration.port()));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (selector.isOpen() && serverChannel.isOpen()) {
            try {
                selector.select();
            } catch (IOException e) {
                continue;
            }

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                try {
                    if (key.isAcceptable()) {
                        SocketChannel client = ((ServerSocketChannel) key.channel()).accept();

                        if (client == null) {
                            continue;
                        }

                        client.configureBlocking(false);

                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);

                        clientKey.attach(ClientChannel.builder()
                                .selectionKey(clientKey)
                                .requestTimer(new TimedOperation(timersScheduler, configuration.requestTimeoutTime(), TimeUnit.SECONDS))
                                .sizeLimits(configuration.sizeLimits())
                                .router(configuration.router())
                                .executorService(executor)
                                .build());
                    } else if (key.isReadable()) {
                        ((ClientChannel) key.attachment()).read();
                    } else if (key.isWritable()) {
                        ((ClientChannel) key.attachment()).flush();
                    }
                } catch (CancelledKeyException | IOException e) {
                    System.out.println("The key has been cancelled: " + e);
                }
            }
        }
    }

    /**
     * Stops the server and shuts down its worker and timer executors.
     */
    public void stop() {
        if (selector != null && selector.isOpen()) {
            selector.wakeup();

            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    System.out.println("Error while closing connection: " + e);
                }
            }

            try {
                selector.close();
            } catch (IOException e) {
                System.out.println("Error while closing selector: " + e);
            }
        }

        selector = null;

        executor.shutdownNow();
        timersScheduler.shutdownNow();
    }
}
