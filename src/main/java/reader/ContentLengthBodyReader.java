package reader;

import common.TimedOperation;
import model.RequestBody;
import client.ClientRequestsQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    protected ContentLengthBodyReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, TimedOperation timedOperation, RequestBody requestBody, long remainingBytes) {
        super(clientRequestsQueue, onReadingAvailable, timedOperation);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(clientRequestsQueue, onReadingAvailable, timedOperation);

            reader.eval(requestByte);

            return new ReadResult(reader, ReadResult.NextAction.PROCEED);
        }

        requestBody.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.enqueue((byte) -1);
            timedOperation.stop();

            return new ReadResult(new RequestLineReader(clientRequestsQueue, onReadingAvailable, timedOperation), ReadResult.NextAction.PROCEED);
        }

        return new ReadResult(this, requestBody.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
