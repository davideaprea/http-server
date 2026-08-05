package reader.dto;

import router.model.Router;
import writer.ResponseWriter;

import java.util.concurrent.ExecutorService;

public record RequestContext(
        Router router,
        ExecutorService executorService,
        ResponseWriter responseWriter
) {
}
