package client.channel;

import client.writer.ContentLengthWriter;
import client.writer.ResponseBodyWriter;
import client.writer.TransferEncodingWriter;
import model.HeaderKey;
import model.Request;
import model.Response;
import router.Router;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ClientRequestsQueue {
    private final Router router;
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Request> requestsQueue = new LinkedList<>();

    private boolean isProcessing = false;

    public ClientRequestsQueue(Router router, ExecutorService executorService, ClientOutputChannel clientOutputChannel) {
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
                    Response response = router.handle(request);

                    clientOutputChannel.write((response + "\r\n").getBytes());

                    ResponseBodyWriter responseBodyWriter;

                    if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                        responseBodyWriter = new ContentLengthWriter(clientOutputChannel);
                    } else {
                        responseBodyWriter = new TransferEncodingWriter(clientOutputChannel);
                    }

                    responseBodyWriter.write(response);

                    return response;
                }, executorService)
                .whenComplete((res, e) -> {
                    isProcessing = false;

                    if (e != null) {
                        throw new RuntimeException(e);
                    }

                    if (!requestsQueue.isEmpty()) {
                        submit(requestsQueue.poll());
                    }
                });
    }
}
