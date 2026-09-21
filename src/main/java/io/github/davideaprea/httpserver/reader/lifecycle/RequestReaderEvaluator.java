package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.reader.dto.ReadResult;
import io.github.davideaprea.httpserver.reader.dto.ReadingError;
import io.github.davideaprea.httpserver.reader.dto.ReadingLifecycleEvents;
import io.github.davideaprea.httpserver.reader.dto.SizeLimits;
import io.github.davideaprea.httpserver.reader.exception.MalformedRequestException;

/**
 * Coordinates the lifecycle of an HTTP request reader by delegating byte
 * processing to the current {@link RequestReader}.
 */
public class RequestReaderEvaluator {
    private final ReadingLifecycleEvents readingLifecycleEvents;
    private final SizeLimits sizeLimits;

    private RequestReader requestReader;

    public RequestReaderEvaluator(ReadingLifecycleEvents readingLifecycleEvents, SizeLimits sizeLimits) {
        this.readingLifecycleEvents = readingLifecycleEvents;
        this.sizeLimits = sizeLimits;

        reset();
    }

    /**
     * Processes a byte using the current request reader and advances the reading
     * lifecycle when appropriate.
     *
     * <p>If an error occurs while processing the byte, the error is reported and
     * the reader lifecycle is reset.</p>
     *
     * @param requestByte the byte read from the request
     * @return {@code true} if reading can proceed, {@code false} otherwise
     */
    public boolean eval(byte requestByte) {
        try {
            ReadResult result = requestReader.eval(requestByte);
            requestReader = result.nextReader();

            return result.canProceed();
        } catch (Exception e) {
            readingLifecycleEvents.onError().accept(new ReadingError(e, e instanceof MalformedRequestException));

            reset();

            return true;
        }
    }

    /**
     * Resets the request reading lifecycle to the beginning of a new request.
     */
    public void reset() {
        requestReader = new RequestLineReader(readingLifecycleEvents, sizeLimits);
    }
}
