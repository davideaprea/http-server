package parser.state;

import parser.dto.ContentLengthRequest;

public class ContentLengthBodyState implements ParsingState {
    private final ContentLengthRequest request;

    private long remainingBytes;

    public ContentLengthBodyState(ContentLengthRequest request) {
        this.request = request;
        remainingBytes = request.bytesNumber();
    }

    @Override
    public ParsingState eval(byte requestByte) {
        if (remainingBytes == 0) {
            request.body().close();

            return new RequestLineState();
        }

        remainingBytes--;

        request.body().append(requestByte);

        return this;
    }
}
