import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import router.Router;
import router.RouterBuilder;
import router.model.RequestHandler;
import server.Server;
import server.ServerConfiguration;
import shared.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ServerTest {
    private Server server;
    private Thread serverThread;

    @BeforeEach
    void setup() {
        Router router = new RouterBuilder()
                .add(new RequestHandler(
                        Method.GET,
                        "/resource/path"
                ) {
                    @Override
                    public Response handle(Request request) {
                        return new Response(
                                Version.HTTP_1_0,
                                Status.OK,
                                Map.of("Content-Type", "text/plain"),
                                "Hello world".getBytes(StandardCharsets.UTF_8)
                        );
                    }
                })
                .build();
        ServerConfiguration serverConfiguration = new ServerConfiguration(0, 3, router);
        server = new Server(serverConfiguration);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void test() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:%d/resource/path".formatted(server.getPort())))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("Hello world", response.body());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
