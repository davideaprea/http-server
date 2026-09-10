package server;

import client.*;
import common.MalformedRequestException;
import common.TimedOperation;
import model.HeaderKey;
import model.Response;
import model.Status;
import model.Version;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
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
                    Client client = (Client) key.attachment();

                    try {
                        client.inputChannel().read();
                    } catch (Exception e) {
                        if (e instanceof MalformedRequestException) {
                            String message = e.getMessage() != null ? e.getMessage() : Status.BAD_REQUEST.getName();
                            Response badRequestResponse = new Response(
                                    Version.HTTP_1_1,
                                    Status.BAD_REQUEST,
                                    Map.of(
                                            HeaderKey.CONNECTION.getValue(), "close",
                                            HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                                            HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(message.length())
                                    ),
                                    new ByteArrayInputStream(message.getBytes())
                            );
                            String rawResponse = badRequestResponse.toHTTPFrame() + badRequestResponse;

                            client.outputChannel().write(rawResponse.getBytes(), true);
                        }

                        client.channelKey().close();
                    }
                } else if (key.isWritable()) {
                    ((Client) key.attachment()).outputChannel().flush();
                }
            }
        }
    }

    private Client createClient(SelectionKey selectionKey) {
        ClientChannelKey clientChannelKey = new ClientChannelKey(selectionKey);
        ClientOutputChannel outputChannel = new ClientOutputChannel(clientChannelKey);
        ClientRequestsQueue clientRequestsQueue = new ClientRequestsQueue(configuration.router(), executor, outputChannel, clientChannelKey);
        TimedOperation timedOperation = new TimedOperation(timersScheduler, configuration.requestTimeoutTime(), TimeUnit.SECONDS, clientChannelKey::close);

        return new Client(
                clientChannelKey,
                new ClientInputChannel(
                        clientChannelKey,
                        timedOperation,
                        clientRequestsQueue
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
