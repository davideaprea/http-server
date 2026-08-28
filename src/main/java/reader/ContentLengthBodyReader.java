package reader;

import common.queue.BodyBytesEnqueue;
import common.queue.RequestQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final BodyBytesEnqueue bodyBytesEnqueue;

    private long remainingBytes;

    public ContentLengthBodyReader(BodyBytesEnqueue body, long bytesNumber, RequestQueue requestQueue) {
        super(requestQueue);

        this.bodyBytesEnqueue = body;
        remainingBytes = bytesNumber;
    }

    @Override
    public RequestReader eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(requestQueue);

            reader.eval(requestByte);

            return reader;
        }

        bodyBytesEnqueue.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            bodyBytesEnqueue.enqueue((byte) -1);

            return new RequestLineReader(requestQueue);
        }

        return this;
    }
}
