package client.dto;

import model.Response;

import java.util.function.Supplier;

public record EnqueuedResponse(
        Supplier<Response> responseSupplier,
        boolean shouldSkipBodyProcessing
) {
}
