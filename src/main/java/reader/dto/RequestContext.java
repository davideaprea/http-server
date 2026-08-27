package reader.dto;

import router.model.Router;
import client.ClientOutputChannel;

import java.util.concurrent.ExecutorService;

public record RequestContext(
        Router router,
        ExecutorService executorService,
        ClientOutputChannel clientOutputChannel
) {
}
