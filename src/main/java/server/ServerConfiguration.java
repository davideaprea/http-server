package server;

import router.model.Router;

public record ServerConfiguration(
        int port,
        int threadPoolSize,
        Router router
) {
}
