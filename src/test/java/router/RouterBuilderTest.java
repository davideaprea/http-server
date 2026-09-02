package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;
import model.Method;

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
}
