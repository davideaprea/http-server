package router;

import model.Method;
import model.Request;
import model.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        Assertions.assertThrows(Throwable.class, () -> routerBuilder.add(handler));
    }
}
