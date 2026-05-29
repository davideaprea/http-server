package router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;
import shared.model.Method;

public class RouterBuilderTest {
    @Test
    void testConflictingHandlersRegistration() {
        HandlerCreateCommand command = new HandlerCreateCommand(
                request -> null,
                Method.GET,
                "/resource/path"
        );
        RouterBuilder routerBuilder = new RouterBuilder().add(command);

        Assertions.assertThrows(ConflictingRoutesException.class, () -> routerBuilder.add(command));
    }
}
