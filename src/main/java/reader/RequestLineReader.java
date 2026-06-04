package reader;

import parser.RequestLineParser;
import parser.RequestTargetParser;
import reader.dto.RequestContext;
import parser.dto.RequestLine;
import reader.util.CRLFSequenceStateTracker;
import shared.model.Request;

public class RequestLineReader extends ReadingState {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.Builder requestBuilder = new Request.Builder();
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();

    public RequestLineReader(RequestContext context) {
        super(context);
    }

    @Override
    public ReadingState eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> CRLFSequenceStateTracker.setLineFeed();
            case '\r' -> {
                CRLFSequenceStateTracker.setCarriageReturn();

                RequestLine requestLine = RequestLineParser.from(requestLineBuilder.toString());

                requestBuilder
                        .method(requestLine.method())
                        .version(requestLine.version())
                        .requestTarget(RequestTargetParser.from(requestLine.requestTarget()));

                return new HeadersReader(requestBuilder, context);
            }
            default -> requestLineBuilder.append(c);
        }

        return this;
    }
}
