package reader;

import client.ClientRequestsQueue;

public abstract class RequestReader {
    protected final ClientRequestsQueue clientRequestsQueue;
    protected final Runnable onReadingAvailable;

    protected RequestReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable) {
        this.clientRequestsQueue = clientRequestsQueue;
        this.onReadingAvailable = onReadingAvailable;
    }

    public abstract ReadResult eval(byte requestByte);
}
