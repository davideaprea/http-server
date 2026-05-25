package parser;

import lombok.AllArgsConstructor;
import parser.dto.Header;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class RequestParser {
    private final ByteBuffer byteBuffer;

    private ParsingState parsingState;
    private StringBuilder currentLine;
    private Request.Builder requestBuilder;
    private boolean isLineFeed;

    public RequestParser(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;

        init();
    }

    public void eval(byte requestByte) {
        switch (parsingState) {
            case REQUEST_LINE -> {
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

                        parsingState = ParsingState.HEADER;
                        RequestLine requestLine = RequestLineParser.from(currentLine.toString());
                        isLineFeed = false;

                        requestBuilder
                                .method(requestLine.method())
                                .version(requestLine.version())
                                .requestTarget(RequestTargetParser.from(requestLine.requestTarget()));
                    }
                    default -> currentLine.append(requestByte);
                }
            }
            case HEADER -> {
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
                            parsingState = ParsingState.BODY;
                        } else {
                            Header header = HeaderParser.from(currentLine.toString());

                            requestBuilder.header(header);
                            currentLine.setLength(0);
                        }

                        isLineFeed = false;
                    }
                    default -> currentLine.append(requestByte);
                }
            }
            case BODY -> {

            }
        }
    }

    private void init() {
        parsingState = ParsingState.REQUEST_LINE;
        currentLine = new StringBuilder();
        requestBuilder = new Request.Builder();
        isLineFeed = false;
    }
}
