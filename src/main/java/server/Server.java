package server;

import parser.RequestParser;
import shared.model.Request;
import shared.model.Response;
import writer.ResponseWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ServerConfiguration configuration;
    private final RequestParser requestParser;

    public Server(ServerConfiguration configuration) {
        this.configuration = configuration;
        this.requestParser = new RequestParser();
    }

    public void init() throws IOException {
        try (
                ServerSocket serverSocket = new ServerSocket(configuration.port());
                ExecutorService executor = Executors.newFixedThreadPool(configuration.threadPoolSize())
        ) {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                executor.submit(() -> {
                    try {
                        Request request = requestParser.from(clientSocket.getInputStream());
                        Response response = configuration.router().handle(request);
                        ResponseWriter responseWriter = new ResponseWriter(clientSocket.getOutputStream());

                        responseWriter.write(response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
