package io.github.davideaprea.httpserver.server;

import lombok.Builder;
import io.github.davideaprea.httpserver.reader.dto.SizeLimits;
import io.github.davideaprea.httpserver.router.Router;

import java.util.Objects;

@Builder
public record ServerConfiguration(
        int port,
        int threadPoolSize,
        Router router,
        long requestTimeoutTime,
        SizeLimits sizeLimits
) {
    public ServerConfiguration {
        Objects.requireNonNull(router);
        Objects.requireNonNull(sizeLimits);
    }
}
