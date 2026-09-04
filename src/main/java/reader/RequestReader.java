package reader;

import client.channel.ClientRequestsQueue;
import common.TimedOperation;

public abstract class RequestReader {
    protected final ClientRequestsQueue clientRequestsQueue;
    protected final Runnable onReadingAvailable;
    protected final TimedOperation timedOperation;

    protected RequestReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, TimedOperation timedOperation) {
        this.clientRequestsQueue = clientRequestsQueue;
        this.onReadingAvailable = onReadingAvailable;
        this.timedOperation = timedOperation;
    }

    public abstract ReadResult eval(byte requestByte);
}
