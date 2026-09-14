package client;

import model.HeaderKey;
import model.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClientRequestsQueue {
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Supplier<Response>> requestsQueue = new LinkedList<>();
    private final Consumer<Exception> onError;

    private boolean isProcessing = false;

    public ClientRequestsQueue(ExecutorService executorService, ClientOutputChannel clientOutputChannel, Consumer<Exception> onError) {
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
        this.onError = onError;
    }

    public void enqueue(Supplier<Response> responseSupplier) {
        synchronized (this) {
            requestsQueue.add(responseSupplier);

            if (isProcessing) {
                return;
            }

            isProcessing = true;
        }

        submitNext();
    }

    private void submitNext() {
        Supplier<Response> request;

        synchronized (this) {
            request = requestsQueue.poll();

            if (request == null) {
                isProcessing = false;

                return;
            }
        }

        executorService.submit(() -> {
            process(request.get());
            submitNext();
        });
    }

    private void process(Response response) {
        clientOutputChannel.write(response.toHTTPFrame().getBytes(), false);

        try (InputStream bodyStream = response.body()) {
            if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                long bytesToWrite = Long.parseLong(response.headers().get(HeaderKey.CONTENT_LENGTH.getValue()));
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bodyStream.read(buffer)) != -1 && bytesToWrite > 0) {
                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead), false);
                    bytesToWrite -= bytesRead;
                }

                if (bytesRead > 0) {
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
        } catch (IOException e) {
            onError.accept(e);
        }
    }
}
