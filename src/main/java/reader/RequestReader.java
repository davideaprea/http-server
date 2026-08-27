package reader;

import common.queue.RequestQueue;

public abstract class RequestReader {
    protected final RequestQueue requestQueue;

    protected RequestReader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public abstract RequestReader eval(byte requestByte);
}
