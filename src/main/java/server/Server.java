package server;

import reader.RequestReader;
import reader.RequestLineReader;
import reader.dto.RequestContext;
import reader.RequestQueue;
import writer.ResponseWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
                    ResponseWriter writer = new ResponseWriter(client, responseWriter -> {
                        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup();
                    });
                    RequestContext requestContext = new RequestContext(configuration.router(), executor, writer);
                    ClientSocketContext context = new ClientSocketContext(
                            writer,
                            new RequestLineReader(new RequestQueue(requestContext))
                    );

                    clientKey.attach(context);
                } else if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ClientSocketContext clientSocketContext = (ClientSocketContext) key.attachment();
                    ByteBuffer buffer = ByteBuffer.allocate(4096);

                    while (socketChannel.read(buffer) > 0) {
                        for (byte reqByte : buffer.array()) {
                            RequestReader nextRequestReader = clientSocketContext
                                    .getRequestReader()
                                    .eval(reqByte);

                            clientSocketContext.setRequestReader(nextRequestReader);
                        }
                    }
                } else if (key.isWritable()) {
                    ((ClientSocketContext) key.attachment()).getResponseWriter().flush();
                }
            }
        }
    }

    public void stop() throws IOException {
        selector.close();
        executor.shutdownNow();
    }
}
