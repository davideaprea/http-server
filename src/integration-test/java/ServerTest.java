import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import router.model.Router;
import router.dto.HandlerCreateCommand;
import server.Server;
import server.ServerConfiguration;
import shared.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ServerTest {
    private Server server;
    private Thread serverThread;

    @BeforeEach
    void setup() {
        Router router = new Router.Builder()
                .add(new HandlerCreateCommand(
                        request -> new Response(
                                Version.HTTP_1_1,
                                Status.OK,
                                Map.of(
                                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                                        HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf("Hello world".length())
                                ),
                                new ByteArrayInputStream("Hello world".getBytes())
                        ),
                        Method.GET,
                        "/resource/path"
                ))
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
                    .uri(URI.create("http://localhost:%d/resource/path".formatted(8000)))
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
