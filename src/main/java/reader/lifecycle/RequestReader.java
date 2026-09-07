package reader.lifecycle;

import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;

public abstract class RequestReader {
    protected final ReadingLifecycleEvents readingLifecycleEvents;

    protected RequestReader(ReadingLifecycleEvents readingLifecycleEvents) {
        this.readingLifecycleEvents = readingLifecycleEvents;
    }

    public abstract ReadResult eval(byte requestByte);
}
