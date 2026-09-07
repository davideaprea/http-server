package client;

import model.HeaderKey;
import model.Request;
import model.Response;
import router.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
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
                    ByteBuffer buffer = ByteBuffer.allocate(8192);

                    try (InputStream bodyStream = response.body()) {
                        if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                            int bytesRead;

                            while ((bytesRead = bodyStream.read(buffer.array())) != -1) {
                                buffer.position(0);
                                buffer.limit(bytesRead);

                                clientOutputChannel.write(buffer);

                                buffer.clear();
                            }
                        } else {
                            response.headers().put(HeaderKey.TRANSFER_ENCODING.getValue(), "chunked");

                            int bytesRead;

                            while ((bytesRead = bodyStream.read(buffer.array())) != -1) {
                                clientOutputChannel.write(ByteBuffer.wrap((Integer.toHexString(bytesRead) + "\r\n").getBytes()));
                                buffer.position(0);
                                buffer.limit(bytesRead);
                                clientOutputChannel.write(buffer);
                                clientOutputChannel.write(ByteBuffer.wrap("\r\n".getBytes()));
                                buffer.clear();
                            }

                            clientOutputChannel.write(ByteBuffer.wrap("0\r\n\r\n".getBytes()));
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    isProcessing = false;

                    if (!requestsQueue.isEmpty()) {
                        submit(requestsQueue.poll());
                    }

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
