package common.queue;

import client.ClientOutputChannel;
import model.Request;
import model.Response;
import model.Status;
import model.Version;
import router.model.Router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RequestQueue {
    private final Router router;
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Request> requestsQueue = new LinkedList<>();

    private boolean isProcessing = false;

    public RequestQueue(Router router, ExecutorService executorService, ClientOutputChannel clientOutputChannel) {
        this.router = router;
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
    }

    public void enqueue(Request request) {
        if (isProcessing) {
            requestsQueue.add(request);
        } else {
            submit(request);
        }
    }

    private void submit(Request request) {
        isProcessing = true;

        CompletableFuture.supplyAsync(() -> {
                    Response response;

                    try {
                        response = router.handle(request);
                    } catch (Exception e) {
                        response = new Response(
                                Version.HTTP_1_1,
                                Status.INTERNAL_SERVER_ERROR,
                                Map.of(),
                                new ByteArrayInputStream(e.getMessage().getBytes())
                        );
                    }

                    clientOutputChannel.write((response + "\r\n").getBytes());

                    byte[] bodyBytes = new byte[8192];

                    try (InputStream bodyStream = response.body()) {
                        while (bodyStream.read(bodyBytes) != -1) {
                            clientOutputChannel.write(bodyBytes);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return response;
                }, executorService)
                .whenComplete((res, ex) -> {
                    isProcessing = false;

                    if (!requestsQueue.isEmpty()) {
                        submit(requestsQueue.poll());
                    }
                });
    }
}
