package io.github.davideaprea.httpserver.router;

import io.github.davideaprea.httpserver.model.*;
import io.github.davideaprea.httpserver.router.Router;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.davideaprea.httpserver.router.dto.HandlerCreateCommand;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RouterTest {
    @Test
    void testValidRequest() {
        Response mockResponse = new Response(
                Version.HTTP_1_1,
                Status.OK,
                Map.of(),
                InputStream.nullInputStream()
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
                new RequestBody(() -> {
                })
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

    @Test
    void testRequestHandlerExceptionHandling() {
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> {
                    throw new RuntimeException();
                },
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

        Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR, response.status());
    }
}
