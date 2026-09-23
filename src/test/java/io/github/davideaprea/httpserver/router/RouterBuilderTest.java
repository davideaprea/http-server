package io.github.davideaprea.httpserver.router;

import io.github.davideaprea.httpserver.model.*;
import io.github.davideaprea.httpserver.router.Router;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.davideaprea.httpserver.router.dto.HandlerCreateCommand;
import io.github.davideaprea.httpserver.router.exception.ConflictingRoutesException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RouterBuilderTest {
    @Test
    void testConflictingHandlersRegistration() {
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        Router.Builder routerBuilder = new Router.Builder().add(command);

        Assertions.assertThrows(ConflictingRoutesException.class, () -> routerBuilder.add(command));
    }

    @Test
    void testHeadEndpointRegistrationForGetRequests() throws IOException {
        String responseBody = "Response body";
        Response handlerResponse = new Response(
                Version.HTTP_1_1,
                Status.OK,
                Map.of(
                        "name", "value",
                        HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(responseBody.length())
                ),
                new ByteArrayInputStream(responseBody.getBytes())
        );
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> handlerResponse,
                Method.GET,
                "/resource/path"
        );
        Router router = new Router.Builder().add(command).build();
        Response actualResponse = router.handle(new Request(
                Method.HEAD,
                Version.HTTP_1_1,
                command.path(),
                Map.of(),
                Map.of(HeaderKey.HOST.getValue(), List.of("host")),
                null
        ));

        Assertions.assertEquals(handlerResponse.status(), actualResponse.status());
        Assertions.assertEquals(handlerResponse.headers(), actualResponse.headers());
        Assertions.assertEquals(0, actualResponse.body().readAllBytes().length);
    }
}
