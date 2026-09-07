package router;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.dto.HandlerCreateCommand;

import java.util.HashMap;
import java.util.Map;

public class RouterTest {
    @Test
    void testValidRequest() {
        Response mockResponse = new Response(
                Version.HTTP_1_1,
                Status.OK,
                Map.of(),
                null
        );
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> mockResponse,
                Method.GET,
                "/resource/path"
        );

        Router router = new Router.Builder()
                .add(command)
                .build();
        Response response = router.handle(new Request(
                command.method(),
                Version.HTTP_1_1,
                command.path(),
                new HashMap<>(),
                new HashMap<>(),
                null
        ));

        Assertions.assertEquals(mockResponse, response);
    }

    @Test
    void testResourceNotFound() {
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        Router router = new Router.Builder()
                .add(command)
                .build();
        Response response = router.handle(new Request(
                command.method(),
                Version.HTTP_1_1,
                "/non/existing/path",
                new HashMap<>(),
                new HashMap<>(),
                null
        ));

        Assertions.assertEquals(Status.NOT_FOUND, response.status());
    }

    @Test
    void testMethodNotSupported() {
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        Router router = new Router.Builder()
                .add(command)
                .build();
        Response response = router.handle(new Request(
                Method.POST,
                Version.HTTP_1_1,
                command.path(),
                new HashMap<>(),
                new HashMap<>(),
                null
        ));

        Assertions.assertEquals(Status.METHOD_NOT_ALLOWED, response.status());
    }
}
