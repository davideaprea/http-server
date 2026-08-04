package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.dto.HandlerCreateCommand;
import router.model.Router;
import shared.exception.ResponseStatusException;
import shared.model.*;

import java.io.InputStream;
import java.util.Map;

public class RouterTest {
    @Test
    void testValidRequest() {
        Response mockResponse = new Response(
                Version.HTTP_1_1,
                Status.OK,
                Map.of(),
                new ResponseBody(InputStream.nullInputStream())
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
                new RequestTarget(command.path(), Map.of()),
                Map.of(),
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
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                command.method(),
                Version.HTTP_1_1,
                new RequestTarget("/non/existing/path", Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_FOUND, ex.getStatus());
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
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                Method.POST,
                Version.HTTP_1_1,
                new RequestTarget(command.path(), Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_IMPLEMENTED, ex.getStatus());
    }
}
