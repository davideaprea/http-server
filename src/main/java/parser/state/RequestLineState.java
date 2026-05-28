package parser.state;

import parser.RequestLineParser;
import parser.RequestTargetParser;
import parser.dto.RequestContext;
import parser.dto.RequestLine;
import shared.model.Request;

public class RequestLineState extends ParsingState {
    private final StringBuilder requestLine = new StringBuilder();
    private final Request.Builder requestBuilder = new Request.Builder();
    private final CRLFSequenceValidator CRLFSequenceValidator = new CRLFSequenceValidator();

    protected RequestLineState(RequestContext context) {
        super(context);
    }

    @Override
    public ParsingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> CRLFSequenceValidator.setLineFeedState();
            case '\r' -> {
                CRLFSequenceValidator.setCarriageReturnState();

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
