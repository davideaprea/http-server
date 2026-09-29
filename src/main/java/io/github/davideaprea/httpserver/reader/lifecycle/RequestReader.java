package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.reader.dto.Context;
import io.github.davideaprea.httpserver.reader.dto.ReadResult;
import io.github.davideaprea.httpserver.reader.dto.ReadingLifecycleEvents;
import io.github.davideaprea.httpserver.reader.dto.SizeLimits;

/**
 * Defines a lifecycle stage for reading an HTTP request.
 *
 * <p>Each reader processes one byte at a time and produces a result that
 * determines the next step in the request reading lifecycle.</p>
 */
public abstract class RequestReader {
    protected final Context context;

    protected RequestReader(Context context) {
        this.context = context;
    }

    /**
     * @param requestByte the byte read from the request
     * @return the result of processing the byte
     */
    public abstract ReadResult eval(byte requestByte);
}
