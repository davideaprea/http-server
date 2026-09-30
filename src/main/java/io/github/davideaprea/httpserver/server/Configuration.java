package io.github.davideaprea.httpserver.server;

import lombok.Builder;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.router.Router;

import java.util.Objects;

@Builder
public record Configuration(
        int port,
        int threadPoolSize,
        Router router,
        long requestTimeoutTime,
        SizeLimits sizeLimits
) {
    public Configuration {
        Objects.requireNonNull(router);
        Objects.requireNonNull(sizeLimits);
    }
}
