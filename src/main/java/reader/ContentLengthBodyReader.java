package reader;

import reader.dto.ContentLengthRequest;
import shared.streaming.RequestQueue;

public class ContentLengthBodyReader extends ReadingState {
    private final ContentLengthRequest request;

    private long remainingBytes;

    public ContentLengthBodyReader(ContentLengthRequest request, RequestQueue requestQueue) {
        super(requestQueue);

        this.request = request;
        remainingBytes = request.bytesNumber();
    }

    @Override
    public ReadingState eval(byte requestByte) {
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
