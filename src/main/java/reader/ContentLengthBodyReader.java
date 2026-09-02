package reader;

import common.queue.RequestBodyBytesQueue;
import client.ClientRequestsQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBodyBytesQueue requestBodyBytesQueue;

    private long remainingBytes;

    protected ContentLengthBodyReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, RequestBodyBytesQueue requestBodyBytesQueue, long remainingBytes) {
        super(clientRequestsQueue, onReadingAvailable);
        this.requestBodyBytesQueue = requestBodyBytesQueue;
        this.remainingBytes = remainingBytes;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(clientRequestsQueue, onReadingAvailable);

            reader.eval(requestByte);

            return new ReadResult(reader, ReadResult.NextAction.PROCEED);
        }

        requestBodyBytesQueue.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBodyBytesQueue.enqueue((byte) -1);

            return new ReadResult(new RequestLineReader(clientRequestsQueue, onReadingAvailable), ReadResult.NextAction.PROCEED);
        }

        return new ReadResult(this, requestBodyBytesQueue.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
