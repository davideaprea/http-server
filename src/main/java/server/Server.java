package server;

import lombok.Getter;
import parser.RequestParser;
import shared.model.Request;
import shared.model.Response;
import shared.model.Status;
import shared.model.Version;
import writer.ResponseWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ServerConfiguration configuration;
    private final RequestParser requestParser;
    private final ExecutorService executor;

    @Getter
    private int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public Server(ServerConfiguration configuration) {
        this.configuration = configuration;
        this.requestParser = new RequestParser();
        executor = Executors.newFixedThreadPool(configuration.threadPoolSize());
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(configuration.port());
        running = true;
        port = serverSocket.getLocalPort();

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                executor.submit(() -> {
                    try (clientSocket) {
                        ResponseWriter responseWriter = new ResponseWriter(clientSocket.getOutputStream());

                        try {
                            Request request = requestParser.from(clientSocket.getInputStream());
                            Response response = configuration.router().handle(request);

                            responseWriter.write(response);
                        } catch (Throwable e) {
                            responseWriter.write(new Response(
                                    Version.HTTP_1_0,
                                    Status.INTERNAL_SERVER_ERROR,
                                    Map.of(),
                                    new byte[0]
                            ));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (java.net.SocketException e) {
                if (!running) break;

                throw e;
            }
        }
    }

    public void stop() throws IOException {
        running = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        executor.shutdownNow();
    }
}
