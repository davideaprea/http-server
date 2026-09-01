package reader;

import common.queue.RequestBodyBytesQueue;
import common.queue.RequestQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBodyBytesQueue requestBodyBytesQueue;

    private long remainingBytes;

    public ContentLengthBodyReader(long bytesNumber, RequestQueue requestQueue, RequestBodyBytesQueue requestBodyBytesQueue) {
        super(requestQueue);

        remainingBytes = bytesNumber;
        this.requestBodyBytesQueue = requestBodyBytesQueue;
    }

    @Override
    public RequestReader eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(requestQueue);

            reader.eval(requestByte);

            return reader;
        }

        requestBodyBytesQueue.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBodyBytesQueue.enqueue((byte) -1);

            return new RequestLineReader(requestQueue);
        }

        return this;
    }
}
