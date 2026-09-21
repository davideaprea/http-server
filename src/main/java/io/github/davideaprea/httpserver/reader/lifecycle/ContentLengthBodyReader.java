package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.model.RequestBody;
import io.github.davideaprea.httpserver.reader.dto.ReadResult;
import io.github.davideaprea.httpserver.reader.dto.ReadingLifecycleEvents;
import io.github.davideaprea.httpserver.reader.dto.SizeLimits;

/**
 * Reads the body of an HTTP request with a known content length.
 */
public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    /**
     * @throws IllegalStateException if the remaining bytes to read ar bigger than the configured body size limit
     */
    public ContentLengthBodyReader(ReadingLifecycleEvents readingLifecycleEvents, RequestBody requestBody, long remainingBytes, SizeLimits sizeLimits) {
        super(readingLifecycleEvents, sizeLimits);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;

        if (remainingBytes > sizeLimits.maxBodySize()) {
            throw new IllegalStateException("Max body size exceeded");
        }
    }


    /**
     * <p>The byte is added to the request body until the expected content length
     * has been reached. Once the body is complete, the request body is closed and
     * the reading lifecycle proceeds to the next request.</p>
     */
    @Override
    public ReadResult eval(byte requestByte) {
        requestBody.enqueue(Byte.toUnsignedInt(requestByte));

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.close();
            readingLifecycleEvents.onEnd().run();

            return new ReadResult(new RequestLineReader(readingLifecycleEvents, sizeLimits), true);
        }

        return new ReadResult(this, !requestBody.isFull());
    }
}
