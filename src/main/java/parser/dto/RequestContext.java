package parser.dto;

import router.Router;

import java.util.concurrent.ExecutorService;

public record RequestContext(
        Router router,
        ExecutorService executorService
) {
}
