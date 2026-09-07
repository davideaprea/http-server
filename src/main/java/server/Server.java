package server;

import client.*;
import common.TimedOperation;
import reader.ReadingLifecycleEvents;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private final ServerConfiguration configuration;
    private final ExecutorService executor;
    private final ScheduledExecutorService timersScheduler = Executors.newScheduledThreadPool(1);

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
                    ClientChannelKey clientChannelKey = new ClientChannelKey(clientKey);
                    ClientOutputChannel outputChannel = new ClientOutputChannel(clientChannelKey);
                    ClientRequestsQueue clientRequestsQueue = new ClientRequestsQueue(configuration.router(), executor, outputChannel);
                    TimedOperation timedOperation = new TimedOperation(timersScheduler, 1, TimeUnit.SECONDS, clientChannelKey::close);

                    clientKey.attach(new Client(
                            new ClientInputChannel(clientChannelKey, ReadingLifecycleEvents.builder()
                                    .onNewRequest(clientRequestsQueue::enqueue)
                                    .onReadingAvailable(() -> {
                                        clientChannelKey.addReadInterest();
                                        clientKey.selector().wakeup();
                                    })
                                    .onStart(timedOperation::start)
                                    .onEnd(timedOperation::stop)
                                    .build()),
                            outputChannel
                    ));
                } else if (key.isReadable()) {
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
        timersScheduler.close();
    }
}
