package io.github.davideaprea.httpserver.connection.dto;

import io.github.davideaprea.httpserver.model.Response;

import java.util.function.Supplier;

public record EnqueuedResponse(
        Supplier<Response> responseSupplier,
        boolean shouldSkipBodyProcessing
) {
}
