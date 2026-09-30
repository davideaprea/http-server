package io.github.davideaprea.httpserver.connection.lifecycle;

import io.github.davideaprea.httpserver.model.RequestBody;
import io.github.davideaprea.httpserver.connection.dto.Context;
import io.github.davideaprea.httpserver.connection.exception.MalformedRequestException;

/**
 * Reads the body of an HTTP request with a known content length.
 */
public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    /**
     * @throws MalformedRequestException if the remaining bytes to read ar bigger than the configured body size limit
     */
    public ContentLengthBodyReader(Context context, RequestBody requestBody, long remainingBytes) {
        super(context);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;

        if (remainingBytes > context.sizeLimits().maxBodySize()) {
            throw new MalformedRequestException("Max body size exceeded");
        }
    }


    /**
     * <p>The byte is added to the request body until the expected content length
     * has been reached. Once the body is complete, the request body is closed and
     * the reading lifecycle proceeds to the next request.</p>
     */
    @Override
    public RequestReader evalNextReader(byte requestByte) {
        requestBody.enqueue(Byte.toUnsignedInt(requestByte));

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.close();
            context.requestTimer().stop();

            return new RequestLineReader(context);
        }

        isFree = !requestBody.isFull();

        return this;
    }
}
