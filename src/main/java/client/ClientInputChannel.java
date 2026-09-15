package client;

import common.TimedOperation;
import model.Response;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;
import reader.exception.MalformedRequestException;
import reader.lifecycle.RequestLineReader;
import reader.lifecycle.RequestReader;
import router.Router;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientInputChannel {
    private final ClientChannelKey clientChannelKey;
    private final ByteBuffer buffer;
    private final ClientResponsesQueue clientResponsesQueue;
    private final ReadingLifecycleEvents readingLifecycleEvents;
    private final SizeLimits sizeLimits;

    private RequestReader requestReader;
    private boolean isFree;

    public ClientInputChannel(ClientChannelKey clientChannelKey, TimedOperation requestTimer, ClientResponsesQueue clientResponsesQueue, SizeLimits sizeLimits, Router router) {
        this.clientChannelKey = clientChannelKey;
        this.clientResponsesQueue = clientResponsesQueue;
        this.sizeLimits = sizeLimits;
        this.readingLifecycleEvents = ReadingLifecycleEvents.builder()
                .onNewRequest(request -> clientResponsesQueue.enqueue(() -> router.handle(request)))
                .onReadingAvailable(() -> {
                    clientChannelKey.addReadInterest();

                    isFree = true;
                })
                .onStart(requestTimer::start)
                .onEnd(requestTimer::stop)
                .build();
        buffer = ByteBuffer.allocateDirect(8192);
        requestReader = new RequestLineReader(readingLifecycleEvents, sizeLimits);
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
            } catch (IOException e) {
                bytesRead = -1;
            }

            if (bytesRead <= 0) break;

            buffer.flip();

            while (buffer.hasRemaining() && isFree) {
                ReadResult readingResult;

                try {
                    readingResult = requestReader.eval(buffer.get());
                } catch (MalformedRequestException e) {
                    clientResponsesQueue.enqueue(() -> Response.badRequestError(e));
                    readingResult = new ReadResult(new RequestLineReader(readingLifecycleEvents, sizeLimits), true);
                } catch (Exception e) {
                    clientChannelKey.close();

                    return;
                }

                requestReader = readingResult.nextReader();
                isFree = readingResult.canProceed();
            }

            buffer.compact();
        }

        if (!isFree) {
            clientChannelKey.removeReadInterest();
        }

        if (bytesRead == -1) {
            clientChannelKey.close();
        }
    }
}
