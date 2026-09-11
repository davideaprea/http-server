package client;

import model.HeaderKey;
import model.Request;
import model.Response;
import router.Router;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class ClientRequestsQueue {
    private final Router router;
    private final ExecutorService executorService;
    private final ClientOutputChannel clientOutputChannel;
    private final Queue<Request> requestsQueue = new LinkedList<>();
    private final ClientChannelKey clientChannelKey;

    private boolean isProcessing = false;

    public ClientRequestsQueue(Router router, ExecutorService executorService, ClientOutputChannel clientOutputChannel, ClientChannelKey clientChannelKey) {
        this.router = router;
        this.executorService = executorService;
        this.clientOutputChannel = clientOutputChannel;
        this.clientChannelKey = clientChannelKey;
    }

    public void enqueue(Request request) {
        synchronized (this) {
            requestsQueue.add(request);

            if (isProcessing) {
                return;
            }

            isProcessing = true;
        }

        submitNext();
    }

    private void submitNext() {
        Request request;

        synchronized (this) {
            request = requestsQueue.poll();

            if (request == null) {
                isProcessing = false;

                return;
            }
        }

        executorService.submit(() -> {
            process(request);
            submitNext();
        });
    }

    private void process(Request request) {
        Response response = router.handle(request);

        clientOutputChannel.write(response.toHTTPFrame().getBytes(), false);

        try (InputStream bodyStream = response.body()) {
            if (response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                long byteToRead = Long.parseLong(response.headers().get(HeaderKey.CONTENT_LENGTH.getValue()));
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bodyStream.read(buffer)) != -1 && byteToRead > 0) {
                    clientOutputChannel.write(Arrays.copyOf(buffer, bytesRead), false);
                    byteToRead--;
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
            clientChannelKey.close();
            requestsQueue.clear();
        }
    }
}
