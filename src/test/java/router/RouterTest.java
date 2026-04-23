package router;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class RouterTest {
    @Test
    void test() {
        Router router = new Router();
        Response mockResponse = new Response(
                Version.HTTP_1_0,
                Status.OK,
                Map.of(),
                new byte[0]
        );
        RequestHandler handler = new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return mockResponse;
            }
        };

        router.add(handler);
        Response response = router.handle(new Request(
                handler.getMethod(),
                Version.HTTP_1_0,
                new RequestTarget(handler.getPath(), Map.of()),
                Map.of(),
                null
        ));

        Assertions.assertEquals(mockResponse, response);
    }

    @Test
    void s() {
        Router router = new Router();
        RequestHandler handler = new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return null;
            }
        };

        router.add(handler);

        Assertions.assertThrows(Throwable.class, () -> router.add(handler));
    }

    @Test
    void d() {
        Router router = new Router();

        router.add(new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return null;
            }
        });

        Assertions.assertThrows(Throwable.class, () -> router.handle(new Request(
                Method.GET,
                Version.HTTP_1_0,
                new RequestTarget("/non/existing/path", Map.of()),
                Map.of(),
                null
        )));
    }
}
