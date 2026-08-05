package shared.streaming;

import reader.dto.RequestContext;
import shared.model.Request;
import shared.model.Response;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class RequestQueue {
    private final RequestContext context;
    private final Queue<Request> requestsQueue = new LinkedList<>();
    private final Map<Request, Response> completedRequests = new HashMap<>();

    public RequestQueue(RequestContext context) {
        this.context = context;
    }

    public void enqueue(Request request) {
        requestsQueue.add(request);

        CompletableFuture.supplyAsync(
                () -> context.router().handle(request),
                context.executorService()
        ).thenAccept(response -> {
            completedRequests.put(request, response);

            processCompletedRequests();
        });
    }

    private void processCompletedRequests() {
        Request request = requestsQueue.peek();

        if (request == null || !completedRequests.containsKey(request)) {
            return;
        }

        Response response = completedRequests.get(request);

        context.responseWriter().accept("%s %s %s\r\n".formatted(
                response.version().getValue(),
                response.status().getCode(),
                response.status().getName()
        ).getBytes());

        for (Map.Entry<String, String> h : response.headers().entrySet()) {
            context.responseWriter().accept("%s: %s\r\n".formatted(h.getKey(), h.getValue()).getBytes());
        }

        context.responseWriter().accept("\r\n".getBytes());

        response.body().subscribe(
                context.responseWriter(),
                () -> {
                    requestsQueue.poll();
                    completedRequests.remove(request);

                    processCompletedRequests();
                }
        );
    }
}
