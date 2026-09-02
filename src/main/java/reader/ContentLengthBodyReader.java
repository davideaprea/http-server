package reader;

import common.queue.RequestBodyBytesQueue;
import common.queue.RequestQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBodyBytesQueue requestBodyBytesQueue;

    private long remainingBytes;

    protected ContentLengthBodyReader(RequestQueue requestQueue, Runnable onReadingAvailable, RequestBodyBytesQueue requestBodyBytesQueue, long remainingBytes) {
        super(requestQueue, onReadingAvailable);
        this.requestBodyBytesQueue = requestBodyBytesQueue;
        this.remainingBytes = remainingBytes;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(requestQueue, onReadingAvailable);

            reader.eval(requestByte);

            return new ReadResult(reader, ReadResult.NextAction.PROCEED);
        }

        requestBodyBytesQueue.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBodyBytesQueue.enqueue((byte) -1);

            return new ReadResult(new RequestLineReader(requestQueue, onReadingAvailable), ReadResult.NextAction.PROCEED);
        }

        return new ReadResult(this, requestBodyBytesQueue.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
