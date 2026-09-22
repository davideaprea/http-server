package io.github.davideaprea.httpserver.client;

import io.github.davideaprea.httpserver.client.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Response;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

    private Future<?> ongoingResponseWriting;
    private boolean isClosed = false;

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
    public synchronized void enqueue(EnqueuedResponse responseSupplier) {
        if (isClosed) {
            return;
        }

        responsesQueue.add(responseSupplier);

        if (ongoingResponseWriting == null) {
            submitNext();
        }
    }

    private synchronized void submitNext() {
        if (isClosed) {
            return;
        }

        ongoingResponseWriting = null;
        EnqueuedResponse enqueuedResponse = responsesQueue.poll();

        if (enqueuedResponse == null) {
            return;
        }

        ongoingResponseWriting = executorService.submit(() -> {
            write(enqueuedResponse);

            if (!Thread.currentThread().isInterrupted()) {
                submitNext();
            }
        });
    }

    private void write(EnqueuedResponse enqueuedResponse) {
        Response response = enqueuedResponse.responseSupplier().get();

        try (InputStream bodyStream = response.body()) {
            clientOutputChannel.write(response.toHTTPFrame().getBytes(), false);

            if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                long bytesToWrite = Long.parseLong(response.headers().get(HeaderKey.CONTENT_LENGTH.getValue()));
                byte[] buffer = new byte[8192];

                while (bytesToWrite > 0) {
                    int maxRead = (int) Math.min(buffer.length, bytesToWrite);
                    int bytesRead = bodyStream.read(buffer, 0, maxRead);

                    if (bytesRead == -1) {
                        onError.accept(new IllegalStateException("Content length hasn't been reached."));

                        return;
                    }

                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead), false);

                    bytesToWrite -= bytesRead;
                }
            } else {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bodyStream.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    clientOutputChannel.write((Integer.toHexString(bytesRead) + "\r\n").getBytes(), false);
                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead), false);
                    clientOutputChannel.write("\r\n".getBytes(), false);
                }

                if (!enqueuedResponse.shouldSkipBodyProcessing()) {
                    clientOutputChannel.write("0\r\n\r\n".getBytes(), false);
                }
            }

            if (response.headers().getOrDefault(HeaderKey.CONNECTION.getValue(), "").equals("close")) {
                clientOutputChannel.write(new byte[0], true);
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public synchronized void close() {
        isClosed = true;

        if (ongoingResponseWriting != null) {
            ongoingResponseWriting.cancel(true);

            ongoingResponseWriting = null;
        }

        responsesQueue.clear();
    }
}
