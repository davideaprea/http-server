import model.Request;
import model.Response;
import parser.RequestParser;
import router.Router;
import writer.ResponseWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final RequestParser requestParser;
    private final Router router;

    public Server(Router router) {
        requestParser = new RequestParser();
        this.router = router;
    }

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
                        Response response = router.handle(request);
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
