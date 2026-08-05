package reader;

import shared.streaming.RequestQueue;

public abstract class ReadingState {
    protected final RequestQueue requestQueue;

    protected ReadingState(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public abstract ReadingState eval(byte requestByte);
}
