package parser.state;

import parser.HeaderParser;
import parser.dto.ContentLengthRequest;
import parser.dto.Header;
import shared.RequestBodyStream;
import shared.exception.ResponseStatusException;
import shared.model.HeaderKey;
import shared.model.Request;
import shared.model.Status;

import java.util.List;
import java.util.Optional;

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
                    Optional<Long> contentLengthValue = Optional
                            .ofNullable(request.headers().get(HeaderKey.CONTENT_LENGTH.getValue()))
                            .map(List::getFirst)
                            .map(Long::parseLong);

                    if (contentLengthValue.isPresent()) {
                        return new ContentLengthBodyState(new ContentLengthRequest(
                                request.body(),
                                contentLengthValue.get()
                        ));
                    }

                    if (request.headers().containsKey(HeaderKey.TRANSFER_ENCODING.getValue())) {
                        return new ChunkedBodyState(request);
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
