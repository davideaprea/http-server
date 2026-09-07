import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import router.dto.HandlerCreateCommand;
import router.Router;
import server.Server;
import server.ServerConfiguration;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

class ServerTest {

    private static final int PORT = 8000;

    private Server server;
    private Thread serverThread;

    @BeforeEach
    void setup() throws Exception {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(
                                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                                        HeaderKey.CONTENT_LENGTH.getValue(), "11"
                                ),
                                new ByteArrayInputStream("Hello world".getBytes())
                        ),
                        Method.GET,
                        "/resource/path"
                ))
                .build();

        server = new Server(new ServerConfiguration(PORT, 3, router, 1));

        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                System.out.println("Test server closed.");
            }
        });

        serverThread.start();

        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void test() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + PORT + "/resource/path"))
                    .GET()
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("Hello world", response.body());
        }
    }
}
