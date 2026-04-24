package router.dto;

import router.model.RequestHandler;
import shared.model.Method;

public record RequestHandlerRegisterCommand(
        RequestHandler handler,
        Method method,
        String path
) {
}
