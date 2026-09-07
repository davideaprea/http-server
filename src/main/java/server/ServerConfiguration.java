package server;

import router.Router;

public record ServerConfiguration(
        int port,
        int threadPoolSize,
        Router router,
        long requestTimeoutTime
) {
}
