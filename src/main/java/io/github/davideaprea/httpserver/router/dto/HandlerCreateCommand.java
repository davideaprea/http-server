package io.github.davideaprea.httpserver.router.dto;

import io.github.davideaprea.httpserver.router.RequestHandler;
import io.github.davideaprea.httpserver.model.Method;

public record HandlerCreateCommand(
        RequestHandler handler,
        Method method,
        String path
) {
}
