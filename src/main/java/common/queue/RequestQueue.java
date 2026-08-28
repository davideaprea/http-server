package common.queue;

import client.ClientOutputChannel;
import common.model.Request;
import common.model.Response;
import router.model.Router;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

public class RequestQueue {
    private final Router router;
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Request> requestsQueue = new ConcurrentLinkedQueue<>();
    private final Map<Request, Response> completedRequests = new ConcurrentHashMap<>();

    public RequestQueue(Router router, ExecutorService executorService, ClientOutputChannel clientOutputChannel) {
        this.router = router;
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
    }

    public void enqueue(Request request) {
        requestsQueue.add(request);

        CompletableFuture.supplyAsync(() -> router.handle(request), executorService)
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

        clientOutputChannel.write((response + "\r\n").getBytes());

        response.body().subscribe(
                bytes -> clientOutputChannel.write(bytes),
                () -> {
                    requestsQueue.poll();
                    completedRequests.remove(request);

                    processCompletedRequests();
                }
        );
    }
}
