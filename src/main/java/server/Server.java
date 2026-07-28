package server;

import reader.ReadingState;
import reader.RequestLineReader;
import reader.dto.RequestContext;
import writer.ResponseWriter;

import java.io.IOException;
import java.io.InputStream;
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

        while (serverChannel.isOpen()) {
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
                    client.register(selector, SelectionKey.OP_READ, new ClientSocketContext(
                            new ResponseWriter(client.socket().getOutputStream()),
                            new RequestLineReader(new RequestContext(configuration.router(), executor))
                    ));
                } else if (key.isReadable()) {
                    InputStream clientInputStream = ((SocketChannel) key.channel()).socket().getInputStream();
                    ClientSocketContext clientSocketContext = (ClientSocketContext) key.attachment();

                    int currentByte;

                    while ((currentByte = clientInputStream.read()) > 0) {
                        ReadingState nextReadingState = clientSocketContext
                                .getReadingState()
                                .eval((byte) currentByte);
                        clientSocketContext.setReadingState(nextReadingState);
                    }
                } else if (key.isWritable()) {

                }
            }
        }
    }

    public void stop() throws IOException {
        selector.close();
        executor.shutdownNow();
    }
}
