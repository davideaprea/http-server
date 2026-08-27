package common.queue;

import reader.dto.RequestContext;
import common.model.Request;
import common.model.Response;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestQueue {
    private final RequestContext context;
    private final Queue<Request> requestsQueue = new ConcurrentLinkedQueue<>();
    private final Map<Request, Response> completedRequests = new ConcurrentHashMap<>();

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
