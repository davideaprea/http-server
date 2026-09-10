package reader.lifecycle;

import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

public abstract class RequestReader {
    protected final ReadingLifecycleEvents readingLifecycleEvents;
    protected final SizeLimits sizeLimits;

    protected RequestReader(ReadingLifecycleEvents readingLifecycleEvents, SizeLimits sizeLimits) {
        this.readingLifecycleEvents = readingLifecycleEvents;
        this.sizeLimits = sizeLimits;
    }

    public abstract ReadResult eval(byte requestByte);
}
