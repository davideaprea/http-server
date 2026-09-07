package reader;

public abstract class RequestReader {
    protected final ReadingLifecycleEvents readingLifecycleEvents;

    protected RequestReader(ReadingLifecycleEvents readingLifecycleEvents) {
        this.readingLifecycleEvents = readingLifecycleEvents;
    }

    public abstract ReadResult eval(byte requestByte);
}
