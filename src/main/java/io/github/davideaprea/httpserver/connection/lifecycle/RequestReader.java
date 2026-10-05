package io.github.davideaprea.httpserver.connection.lifecycle;

import io.github.davideaprea.httpserver.connection.channel.ClientChannel;
import io.github.davideaprea.httpserver.connection.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.model.Response;
import io.github.davideaprea.httpserver.connection.exception.MalformedRequestException;

import java.nio.channels.SelectionKey;

/**
 * Defines a lifecycle stage for reading an HTTP request.
 *
 * <p>Each reader processes one byte at a time and produces a result that
 * determines the next step in the request reading lifecycle.</p>
 */
public abstract class RequestReader {
    protected final ClientChannel clientChannel;

    protected RequestReader(ClientChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    /**
     * @param requestByte the byte read from the request
     * @return the result of processing the byte
     */
    public RequestReader evaluate(byte requestByte) {
        try {
            RequestReader requestReader = evalNextReader(requestByte);

            if (!isFree()) {
                clientChannel.disableInterest(SelectionKey.OP_READ);
            }

            return requestReader;
        } catch (Exception e) {
            if (e instanceof MalformedRequestException) {
                clientChannel.enqueue(new EnqueuedResponse(
                        () -> Response.badRequestError(e),
                        false
                ));
            } else {
                clientChannel.close();
            }

            return new RequestLineReader(clientChannel);
        }
    }

    protected abstract RequestReader evalNextReader(byte requestByte);

    public boolean isFree() {
        return true;
    }
}
