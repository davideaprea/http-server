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
}
