package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.exception.ConflictingRoutesException;
import router.model.RequestHandler;
import shared.model.Method;
import shared.model.Request;
import shared.model.Response;

public class RouterBuilderTest {
    @Test
    void testConflictingHandlersRegistration() {
        RequestHandler handler = new RequestHandler(
                Method.GET,
                "/resource/path"
        ) {
            @Override
            public Response handle(Request request) {
                return null;
            }
        };
        RouterBuilder routerBuilder = new RouterBuilder().add(handler);

        Assertions.assertThrows(ConflictingRoutesException.class, () -> routerBuilder.add(handler));
    }
}
