package reader;

import common.queue.RequestQueue;
import reader.dto.ContentLengthRequest;

public class ContentLengthBodyReader extends RequestReader {
    private final ContentLengthRequest request;

    private long remainingBytes;

    public ContentLengthBodyReader(ContentLengthRequest request, RequestQueue requestQueue) {
        super(requestQueue);

        this.request = request;
        remainingBytes = request.bytesNumber();
    }

    @Override
    public RequestReader eval(byte requestByte) {
        if (remainingBytes == 0) {
            throw new IllegalStateException("Content length has already been reached.");
        }

        remainingBytes--;

        if (remainingBytes == 0) {
            request.body().enqueue((byte) -1);

            return new RequestLineReader(requestQueue);
        } else {
            request.body().enqueue(requestByte);
        }

        return this;
    }
}
