package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.dto.RequestHandlerRegisterCommand;
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
        RequestHandlerRegisterCommand command = new RequestHandlerRegisterCommand(
                request -> mockResponse,
                Method.GET,
                "/resource/path"
        );

        Router router = new RouterBuilder()
                .add(command)
                .build();
        Response response = router.handle(new Request(
                command.method(),
                Version.HTTP_1_0,
                new RequestTarget(command.path(), Map.of()),
                Map.of(),
                null
        ));

        Assertions.assertEquals(mockResponse, response);
    }

    @Test
    void testResourceNotFound() {
        RequestHandlerRegisterCommand command = new RequestHandlerRegisterCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        Router router = new RouterBuilder()
                .add(command)
                .build();
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                command.method(),
                Version.HTTP_1_0,
                new RequestTarget("/non/existing/path", Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testMethodNotSupported() {
        RequestHandlerRegisterCommand command = new RequestHandlerRegisterCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        Router router = new RouterBuilder()
                .add(command)
                .build();
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> router.handle(new Request(
                Method.POST,
                Version.HTTP_1_0,
                new RequestTarget(command.path(), Map.of()),
                Map.of(),
                null
        )));

        Assertions.assertEquals(Status.NOT_IMPLEMENTED, ex.getStatus());
    }
}
