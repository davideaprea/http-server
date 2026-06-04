package reader;

import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;

public class ContentLengthBodyReader extends ReadingState {
    private final ContentLengthRequest request;

    private long remainingBytes;

    public ContentLengthBodyReader(ContentLengthRequest request, RequestContext context) {
        super(context);

        this.request = request;
        remainingBytes = request.bytesNumber();
    }

    @Override
    public ReadingState eval(byte requestByte) {
        if (remainingBytes == 0) {
            request.body().close();

            return new RequestLineReader(context);
        }

        remainingBytes--;

        request.body().append(requestByte);

        return this;
    }
}
