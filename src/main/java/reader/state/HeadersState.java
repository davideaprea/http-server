package reader.state;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;
import shared.RequestBodyStream;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

import java.util.Optional;

public class HeadersState extends ParsingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final CRLFSequenceState CRLFSequenceState = new CRLFSequenceState();

    public HeadersState(Request.Builder requestBuilder, RequestContext context) {
        super(context);

        this.requestBuilder = requestBuilder;
    }

    @Override
    public ParsingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> CRLFSequenceState.setLineFeed();
            case '\r' -> {
                CRLFSequenceState.setCarriageReturn();

                if (currentLine.isEmpty()) {
                    Request request = requestBuilder.body(new RequestBodyStream()).build();
                    Optional<Long> contentLengthValue = request.getContentLength();
                    Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

                    if (contentLengthValue.isEmpty() && transferEncodingValue.isEmpty()) {
                        throw new ResponseStatusException("", Status.BAD_REQUEST);
                    }

                    if (contentLengthValue.isPresent()) {
                        return new ContentLengthBodyState(new ContentLengthRequest(
                                request.body(),
                                contentLengthValue.get()
                        ), context);
                    }

                    return new ChunkedBodyState(request.body(), context);
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    requestBuilder.header(header);
                    currentLine.setLength(0);
                }
            }
            default -> currentLine.append(requestByte);
        }

        return this;
    }
}
