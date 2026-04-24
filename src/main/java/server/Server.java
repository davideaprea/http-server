package server;

import lombok.Getter;
import parser.RequestParser;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Response;
import shared.model.Status;
import shared.model.Version;
import writer.ResponseWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ServerConfiguration configuration;
    private final RequestParser requestParser;
    private final ExecutorService executor;

    @Getter
    private int port;
    private ServerSocket serverSocket;

    public Server(ServerConfiguration configuration) {
        this.configuration = configuration;
        this.requestParser = new RequestParser();
        executor = Executors.newFixedThreadPool(configuration.threadPoolSize());
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(configuration.port());
        port = serverSocket.getLocalPort();

        while (isServerSocketOpen()) {
            Socket clientSocket;

            try {
                clientSocket = serverSocket.accept();
            } catch (SocketException e) {
                if (serverSocket.isClosed()) {
                    break;
                }

                throw e;
            }

            executor.submit(() -> {
                ResponseWriter responseWriter = new ResponseWriter(clientSocket);

                try {
                    Request request = requestParser.from(clientSocket);
                    Response response = configuration.router().handle(request);

                    responseWriter.write(response);
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                } catch (ResponseStatusException e) {
                    responseWriter.write(new Response(
                            Version.HTTP_1_1,
                            e.getStatus(),
                            Map.of(),
                            new byte[0]
                    ));
                } catch (Exception e) {
                    responseWriter.write(new Response(
                            Version.HTTP_1_1,
                            Status.INTERNAL_SERVER_ERROR,
                            Map.of(),
                            new byte[0]
                    ));
                }
            });
        }
    }

    public void stop() throws IOException {
        if (isServerSocketOpen()) {
            serverSocket.close();
        }

        executor.shutdownNow();
    }

    private boolean isServerSocketOpen() {
        return serverSocket != null && !serverSocket.isClosed();
    }
}
