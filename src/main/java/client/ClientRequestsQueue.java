package client;

import model.HeaderKey;
import model.Request;
import model.Response;
import router.Router;
import writer.ContentLengthWriter;
import writer.ResponseBodyWriter;
import writer.TransferEncodingWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

public class ClientRequestsQueue {
    private final Router router;
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Request> requestsQueue = new ConcurrentLinkedQueue<>();
    private final ClientChannelKey clientChannelKey;

    private boolean isProcessing = false;

    public ClientRequestsQueue(Router router, ExecutorService executorService, ClientOutputChannel clientOutputChannel, ClientChannelKey clientChannelKey) {
        this.router = router;
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
        this.clientChannelKey = clientChannelKey;
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
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            ResponseBodyWriter responseBodyWriter;

            if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                responseBodyWriter = new ContentLengthWriter(clientOutputChannel);
            } else {
                response.headers().put(HeaderKey.TRANSFER_ENCODING.getValue(), "chunked");

                responseBodyWriter = new TransferEncodingWriter(clientOutputChannel);
            }

            clientOutputChannel.write(ByteBuffer.wrap((response.toHTTPFrame()).getBytes()));

            try (InputStream bodyStream = response.body()) {
                responseBodyWriter.fromSource(bodyStream);
            } catch (IOException e) {
                clientChannelKey.close();
                requestsQueue.clear();

                return response;
            }

            isProcessing = false;

            if (!requestsQueue.isEmpty()) {
                submit(requestsQueue.poll());
            }

            return response;
        }, executorService);
    }
}
