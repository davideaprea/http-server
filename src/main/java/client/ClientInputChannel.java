package client;

import common.MalformedRequestException;
import common.TimedOperation;
import model.HeaderKey;
import model.Response;
import model.Status;
import model.Version;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.lifecycle.RequestLineReader;
import reader.lifecycle.RequestReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ClientInputChannel {
    private final ClientChannelKey clientChannelKey;
    private final ByteBuffer buffer;

    private RequestReader requestReader;
    private boolean isFree;

    public ClientInputChannel(ClientChannelKey clientChannelKey, TimedOperation requestTimer, ClientRequestsQueue clientRequestsQueue) {
        this.clientChannelKey = clientChannelKey;
        buffer = ByteBuffer.allocateDirect(8192);
        requestReader = new RequestLineReader(ReadingLifecycleEvents.builder()
                .onNewRequest(clientRequestsQueue::enqueue)
                .onReadingAvailable(() -> {
                    clientChannelKey.addReadInterest();

                    isFree = true;
                })
                .onStart(requestTimer::start)
                .onEnd(requestTimer::stop)
                .build());
        isFree = true;
    }

    public void read() {
        if (!isFree) {
            return;
        }

        SocketChannel client = clientChannelKey.getSocketChannel();
        int bytesRead;

        while (true) {
            try {
                bytesRead = client.read(buffer);

                if (bytesRead <= 0) break;

                buffer.flip();

                while (buffer.hasRemaining() && isFree) {
                    ReadResult readingResult = requestReader.eval(buffer.get());
                    requestReader = readingResult.nextReader();
                    isFree = readingResult.canProceed();
                }

                buffer.compact();
            } catch (Exception e) {
                if (e instanceof MalformedRequestException) {
                    sendCloseResponse(e.getMessage());
                }

                clientChannelKey.close();
            }
        }

        if (!isFree) {
            clientChannelKey.removeReadInterest();
        }

        if (bytesRead == -1) {
            clientChannelKey.close();
        }
    }

    private void sendCloseResponse(String message) {
        Response response = new Response(
                Version.HTTP_1_1,
                Status.BAD_REQUEST,
                Map.of(
                        HeaderKey.CONNECTION.getValue(), "close",
                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                        HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(message.length())
                ),
                new ByteArrayInputStream(message.getBytes())
        );
        String rawResponse = response.toHTTPFrame() + response;

        try {
            clientChannelKey.getSocketChannel().write(ByteBuffer.wrap(rawResponse.getBytes()));
        } catch (IOException e) {
            System.out.println("Connection's closed.");
        }
    }
}
