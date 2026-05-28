package reader.state;

import parser.RequestLineParser;
import parser.RequestTargetParser;
import reader.dto.RequestContext;
import parser.dto.RequestLine;
import shared.model.Request;

public class RequestLineState extends ParsingState {
    private final StringBuilder requestLine = new StringBuilder();
    private final Request.Builder requestBuilder = new Request.Builder();
    private final CRLFSequenceState CRLFSequenceState = new CRLFSequenceState();

    protected RequestLineState(RequestContext context) {
        super(context);
    }

    @Override
    public ParsingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> CRLFSequenceState.setLineFeed();
            case '\r' -> {
                CRLFSequenceState.setCarriageReturn();

                RequestLine requestLine = RequestLineParser.from(RequestLineState.this.requestLine.toString());

                requestBuilder
                        .method(requestLine.method())
                        .version(requestLine.version())
                        .requestTarget(RequestTargetParser.from(requestLine.requestTarget()));

                return new HeadersState(requestBuilder, context);
            }
            default -> requestLine.append(requestByte);
        }

        return this;
    }
}
