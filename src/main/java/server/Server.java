package server;

import client.*;
import common.TimedOperation;
import model.Response;
import reader.dto.ReadingLifecycleEvents;
import reader.lifecycle.RequestReaderEvaluator;

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
    private final ScheduledExecutorService timersScheduler;

    private Selector selector;

    public Server(ServerConfiguration configuration) {
        this.configuration = configuration;
        executor = Executors.newFixedThreadPool(configuration.threadPoolSize());
        timersScheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverChannel = createServerChannel();

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

                    clientKey.attach(createClient(clientKey));
                } else if (key.isReadable()) {
                    ((Client) key.attachment()).inputChannel().read();
                } else if (key.isWritable()) {
                    ((Client) key.attachment()).outputChannel().flush();
                }
            }
        }
    }

    private Client createClient(SelectionKey selectionKey) {
        ClientChannelKey clientChannelKey = new ClientChannelKey(selectionKey);
        ClientOutputChannel outputChannel = new ClientOutputChannel(clientChannelKey);
        ClientResponsesQueue clientResponsesQueue = new ClientResponsesQueue(executor, outputChannel, e -> clientChannelKey.close());
        TimedOperation requestTimer = new TimedOperation(timersScheduler, configuration.requestTimeoutTime(), TimeUnit.SECONDS, clientChannelKey::close);
        ReadingLifecycleEvents readingLifecycleEvents = ReadingLifecycleEvents.builder()
                .onNewRequest(request -> clientResponsesQueue.enqueue(() -> configuration.router().handle(request)))
                .onReadingAvailable(clientChannelKey::addReadInterest)
                .onStart(requestTimer::start)
                .onEnd(requestTimer::stop)
                .onError(error -> {
                    if (error.isRecoverable()) {
                        clientResponsesQueue.enqueue(() -> Response.badRequestError(error.value()));
                    } else {
                        clientChannelKey.close();
                    }
                })
                .build();

        return new Client(
                new ClientInputChannel(
                        clientChannelKey,
                        new RequestReaderEvaluator(readingLifecycleEvents, configuration.sizeLimits())
                ),
                outputChannel
        );
    }

    private ServerSocketChannel createServerChannel() throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(configuration.port()));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        return serverChannel;
    }

    public void stop() throws IOException {
        selector.close();
        executor.shutdownNow();
        timersScheduler.close();
    }
}
