package reader;

import common.queue.RequestQueue;

public abstract class RequestReader {
    protected final RequestQueue requestQueue;
    protected final Runnable onReadingAvailable;

    protected RequestReader(RequestQueue requestQueue, Runnable onReadingAvailable) {
        this.requestQueue = requestQueue;
        this.onReadingAvailable = onReadingAvailable;
    }

    public abstract ReadResult eval(byte requestByte);
}
