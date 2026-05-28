package parser.state;

import shared.model.HeaderKey;
import shared.model.Request;

public class ContentLengthBodyState implements ParsingState {
    private final Request request;

    private long remainingBytes;

    protected ContentLengthBodyState(Request request) {
        this.request = request;
        this.remainingBytes = Long.parseLong(request.headers().get(HeaderKey.CONTENT_LENGTH.getValue()).getFirst());
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
