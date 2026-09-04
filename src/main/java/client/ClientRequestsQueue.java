package client;

import model.*;
import router.Router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.Map;
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

                    if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                        byte[] bodyBytes = new byte[8192];

                        try (InputStream bodyStream = response.body()) {
                            while (bodyStream.read(bodyBytes) != -1) {
                                clientOutputChannel.write(bodyBytes);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else {
                        byte[] bodyBytes = new byte[8192];

                        try (InputStream bodyStream = response.body()) {
                            int totalBytesRead;

                            while ((totalBytesRead = bodyStream.read(bodyBytes)) != -1) {
                                clientOutputChannel.write(String.valueOf(totalBytesRead).getBytes());
                                clientOutputChannel.write("\r\n".getBytes());
                                clientOutputChannel.write(bodyBytes);
                                clientOutputChannel.write("\r\n".getBytes());
                            }

                            clientOutputChannel.write("0\r\n\r\n".getBytes());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
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
