package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.model.RequestHandler;
import shared.exception.ResponseStatusException;
import shared.model.*;

import java.util.Map;

public class RouterTest {
    @Test
    void testValidRequest() {
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

        Router router = new RouterBuilder()
                .add(handler)
                .build();
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
    void testResourceNotFound() {
        RequestHandler handler = new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return null;
            }
        };
        Router router = new RouterBuilder()
                .add(handler)
                .build();
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                handler.getMethod(),
                Version.HTTP_1_0,
                new RequestTarget("/non/existing/path", Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testMethodNotSupported() {
        RequestHandler handler = new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return null;
            }
        };
        Router router = new RouterBuilder()
                .add(handler)
                .build();
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                Method.POST,
                Version.HTTP_1_0,
                new RequestTarget(handler.getPath(), Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_IMPLEMENTED, ex.getStatus());
    }
}
