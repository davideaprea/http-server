package parser.state;

import parser.dto.ContentLengthRequest;
import parser.dto.RequestContext;

public class ContentLengthBodyState extends ParsingState {
    private final ContentLengthRequest request;

    private long remainingBytes;

    public ContentLengthBodyState(ContentLengthRequest request, RequestContext context) {
        super(context);

        this.request = request;
        remainingBytes = request.bytesNumber();
    }

    @Override
    public ParsingState eval(byte requestByte) {
        if (remainingBytes == 0) {
            request.body().close();

            return new RequestLineState(context);
        }

        remainingBytes--;

        request.body().append(requestByte);

        return this;
    }
}
