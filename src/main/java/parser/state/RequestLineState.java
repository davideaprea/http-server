package parser.state;

import parser.RequestLineParser;
import parser.RequestTargetParser;
import parser.dto.RequestContext;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

public class RequestLineState extends ParsingState {
    private final StringBuilder requestLine = new StringBuilder();
    private final Request.Builder requestBuilder = new Request.Builder();

    private boolean isLineFeed = false;

    protected RequestLineState(RequestContext context) {
        super(context);
    }

    @Override
    public ParsingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> {
                if (isLineFeed) {
                    throw new ResponseStatusException("", Status.BAD_REQUEST);
                }

                isLineFeed = true;
            }
            case '\r' -> {
                if (!isLineFeed) {
                    throw new ResponseStatusException("", Status.BAD_REQUEST);
                }

                RequestLine requestLine = RequestLineParser.from(RequestLineState.this.requestLine.toString());
                isLineFeed = false;

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
