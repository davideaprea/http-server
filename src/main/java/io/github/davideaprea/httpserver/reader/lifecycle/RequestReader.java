package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.client.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.model.Response;
import io.github.davideaprea.httpserver.reader.dto.Context;
import io.github.davideaprea.httpserver.reader.exception.MalformedRequestException;
import lombok.Getter;

/**
 * Defines a lifecycle stage for reading an HTTP request.
 *
 * <p>Each reader processes one byte at a time and produces a result that
 * determines the next step in the request reading lifecycle.</p>
 */
public abstract class RequestReader {
    protected final Context context;

    @Getter
    protected boolean isFree = true;

    protected RequestReader(Context context) {
        this.context = context;
    }

    public RequestReader evaluate(byte requestByte) {
        try {
            RequestReader requestReader = evalNextReader(requestByte);

            if (!isFree) {
                context.channelKey().removeReadInterest();
            }

            return requestReader;
        } catch (Exception e) {
            if (e instanceof MalformedRequestException) {
                context.responsesQueue().enqueue(new EnqueuedResponse(
                        () -> Response.badRequestError(e),
                        false
                ));
            } else {
                context.channelKey().close();
            }

            return new RequestLineReader(context);
        }
    }

    public void close() {
        context.requestTimer().stop();
        context.channelKey().close();
        context.responsesQueue().close();
    }

    /**
     * @param requestByte the byte read from the request
     * @return the result of processing the byte
     */
    protected abstract RequestReader evalNextReader(byte requestByte);
}
