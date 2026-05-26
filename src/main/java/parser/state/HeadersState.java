package parser.state;

import parser.HeaderParser;
import parser.dto.Header;
import shared.RequestBodyStream;
import shared.exception.ResponseStatusException;
import shared.model.HeaderKey;
import shared.model.Request;
import shared.model.Status;

public class HeadersState implements ParsingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();

    private boolean isLineFeed = false;

    public HeadersState(Request.Builder requestBuilder) {
        this.requestBuilder = requestBuilder;
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

                if (currentLine.isEmpty()) {
                    Request request = requestBuilder.body(new RequestBodyStream()).build();

                    if (request.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue())) {
                        return new ContentLengthBodyState(request);
                    }
                    if (request.headers().containsKey(HeaderKey.TRANSFER_ENCODING.getValue())) {
                        return new TransferEncodingBodyState(request);
                    }

                    //throw
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    requestBuilder.header(header);
                    currentLine.setLength(0);
                }

                isLineFeed = false;
            }
            default -> currentLine.append(requestByte);
        }

        return this;
    }
}
