package common.queue;

import common.model.Request;
import common.model.Response;
import reader.dto.RequestContext;

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
                )
                .thenAccept(response -> {
                    completedRequests.put(request, response);

                    processCompletedRequests();
                })
                .handle((res, ex) -> {
                    if (ex != null) {
                        return ex;
                    }

                    return res;
                });
    }

    private void processCompletedRequests() {
        Request request = requestsQueue.peek();

        if (request == null || !completedRequests.containsKey(request)) {
            return;
        }

        Response response = completedRequests.get(request);

        context.clientOutputChannel().accept((response + "\r\n").getBytes());

        response.body().subscribe(
                context.clientOutputChannel(),
                () -> {
                    requestsQueue.poll();
                    completedRequests.remove(request);

                    processCompletedRequests();
                }
        );
    }
}
