package io.github.davideaprea.httpserver.server;

import io.github.davideaprea.httpserver.client.ClientChannelKey;
import io.github.davideaprea.httpserver.client.ClientInputChannel;
import io.github.davideaprea.httpserver.client.ClientOutputChannel;
import io.github.davideaprea.httpserver.client.ClientResponsesQueue;
import io.github.davideaprea.httpserver.client.dto.Client;
import io.github.davideaprea.httpserver.client.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Method;
import io.github.davideaprea.httpserver.model.Response;
import io.github.davideaprea.httpserver.reader.dto.ReadingLifecycleEvents;
import io.github.davideaprea.httpserver.reader.lifecycle.RequestReaderEvaluator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs an HTTP server that accepts client connections and processes HTTP
 * requests using a non-blocking I/O model.
 */
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

    /**
     * Starts the server and processes client connections and I/O events.
     *
     * <p>This method blocks while the server is running.</p>
     *
     * @throws IOException if an I/O error occurs while initializing or processing
     *                     the server
     */
    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverChannel = createServerChannel();

        while (selector.isOpen() && serverChannel.isOpen()) {
            selector.select();

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

                        clientKey.attach(createClient(clientKey));
                    } else if (key.isReadable()) {
                        ((Client) key.attachment()).inputChannel().read();
                    } else if (key.isWritable()) {
                        ((Client) key.attachment()).outputChannel().flush();
                    }
                } catch (CancelledKeyException | ClosedChannelException e) {
                    System.out.println("The key has been cancelled: " + e);
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
                .onNewRequest(request -> clientResponsesQueue.enqueue(new EnqueuedResponse(
                        () -> {
                            Response response = configuration.router().handle(request);

                            if (request.isClosingRequest()) {
                                response.headers().put(HeaderKey.CONNECTION.getValue(), "close");
                            }

                            return response;
                        },
                        Method.HEAD.equals(request.getMethod())
                )))
                .onReadingAvailable(clientChannelKey::addReadInterest)
                .onStart(requestTimer::start)
                .onEnd(requestTimer::stop)
                .onError(error -> {
                    if (error.isRecoverable()) {
                        clientResponsesQueue.enqueue(new EnqueuedResponse(
                                () -> Response.badRequestError(error.value()),
                                false
                        ));
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

    /**
     * Stops the server and shuts down its worker and timer executors.
     *
     * @throws IOException if an I/O error occurs while closing the selector
     */
    public void stop() throws IOException {
        if (selector != null && selector.isOpen()) {
            selector.wakeup();

            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    System.out.println("Error while closing channel: " + e);
                }
            }

            selector.close();
        }

        executor.shutdownNow();
        timersScheduler.shutdownNow();
    }
}
