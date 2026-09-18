package reader.lifecycle;

import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

/**
 * Defines a lifecycle stage for reading an HTTP request.
 *
 * <p>Each reader processes one byte at a time and produces a result that
 * determines the next step in the request reading lifecycle.</p>
 */
public abstract class RequestReader {
    protected final ReadingLifecycleEvents readingLifecycleEvents;
    protected final SizeLimits sizeLimits;

    protected RequestReader(ReadingLifecycleEvents readingLifecycleEvents, SizeLimits sizeLimits) {
        this.readingLifecycleEvents = readingLifecycleEvents;
        this.sizeLimits = sizeLimits;
    }

    /**
     * @param requestByte the byte read from the request
     * @return the result of processing the byte
     */
    public abstract ReadResult eval(byte requestByte);
}
