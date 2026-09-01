package router.dto;

import router.model.RequestHandler;
import model.Method;

public record HandlerCreateCommand(
        RequestHandler handler,
        Method method,
        String path
) {
}
