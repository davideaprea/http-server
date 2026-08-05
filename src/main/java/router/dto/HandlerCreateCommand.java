package router.dto;

import router.model.RequestHandler;
import common.model.Method;

public record HandlerCreateCommand(
        RequestHandler handler,
        Method method,
        String path
) {
}
