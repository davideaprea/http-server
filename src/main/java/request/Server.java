package request;

import request.model.Request;
import request.parser.RequestParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final RequestParser requestParser = new RequestParser();

    public void init() throws IOException {
        try (
                ServerSocket serverSocket = new ServerSocket(8080);
                ExecutorService executor = Executors.newFixedThreadPool(5)
        ) {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                executor.submit(() -> {
                    try {
                        Request request = requestParser.from(clientSocket.getInputStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
