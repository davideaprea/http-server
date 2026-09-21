package io.github.davideaprea.httpserver.client;

import io.github.davideaprea.httpserver.client.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Response;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Queues HTTP responses for sequential processing and writing to a client.
 *
 * <p>Responses are processed asynchronously and written to the client in the
 * same order in which they are enqueued.</p>
 */
public class ClientResponsesQueue {
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<EnqueuedResponse> responsesQueue = new LinkedList<>();
    private final Consumer<Exception> onError;

    private boolean isProcessing = false;

    public ClientResponsesQueue(ExecutorService executorService, ClientOutputChannel clientOutputChannel, Consumer<Exception> onError) {
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
        this.onError = onError;
    }

    /**
     * Adds a response supplier to the processing queue.
     *
     * <p>If no response is currently being processed, processing starts
     * immediately.</p>
     */
    public void enqueue(EnqueuedResponse responseSupplier) {
        synchronized (this) {
            responsesQueue.add(responseSupplier);

            if (isProcessing) {
                return;
            }

            isProcessing = true;
        }

        submitNext();
    }

    private void submitNext() {
        EnqueuedResponse enqueuedResponse;

        synchronized (this) {
            enqueuedResponse = responsesQueue.poll();

            if (enqueuedResponse == null) {
                isProcessing = false;

                return;
            }
        }

        executorService.submit(() -> {
            write(enqueuedResponse);
            submitNext();
        });
    }

    private void write(EnqueuedResponse enqueuedResponse) {
        Response response = enqueuedResponse.responseSupplier().get();
        clientOutputChannel.write(response.toHTTPFrame().getBytes(), false);

        try (InputStream bodyStream = response.body()) {
            if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                long bytesToWrite = Long.parseLong(response.headers().get(HeaderKey.CONTENT_LENGTH.getValue()));
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bodyStream.read(buffer)) != -1 && bytesToWrite > 0) {
                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead > bytesToWrite ? (int) bytesToWrite : bytesRead), false);
                    bytesToWrite -= bytesRead;
                }

                if (!enqueuedResponse.shouldSkipBodyProcessing() && bytesToWrite > 0) {
                    onError.accept(new IllegalStateException("Content length hasn't been reached."));
                }
            } else {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bodyStream.read(buffer)) != -1) {
                    clientOutputChannel.write((Integer.toHexString(bytesRead) + "\r\n").getBytes(), false);
                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead), false);
                    clientOutputChannel.write("\r\n".getBytes(), false);
                }

                clientOutputChannel.write("0\r\n\r\n".getBytes(), false);
            }

            if (response.headers().getOrDefault(HeaderKey.CONNECTION.getValue(), "").equals("close")) {
                clientOutputChannel.write(new byte[0], true);
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }
}
