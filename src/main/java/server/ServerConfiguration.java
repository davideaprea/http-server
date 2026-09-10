package server;

import lombok.Builder;
import reader.dto.SizeLimits;
import router.Router;

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
