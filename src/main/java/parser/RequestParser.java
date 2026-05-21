package parser;

import parser.dto.Header;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.HeaderKey;
import shared.model.Method;
import shared.model.Request;
import shared.model.Status;

import java.io.InputStream;

public class RequestParser {
    private ParsingState parsingState;
    private StringBuilder currentLine;
    private Request.Builder requestBuilder;
    private boolean isLineFeed;

    public RequestParser() {
        init();
    }

    public void eval(char character) {
        switch (character) {
            case '\n' -> {
                if (isLineFeed) {
                    //throw
                }

                isLineFeed = true;
            }
            case '\r' -> {
                if (!isLineFeed) {
                    //throw
                }

                switch (parsingState) {
                    case REQUEST_LINE -> {
                        parsingState = ParsingState.HEADER;
                        RequestLine requestLine = RequestLineParser.from(currentLine.toString());

                        requestBuilder
                                .method(requestLine.method())
                                .version(requestLine.version())
                                .requestTarget(RequestTargetParser.from(requestLine.requestTarget()));
                    }
                    case HEADER -> {
                        Header header = HeaderParser.from(currentLine.toString());

                        requestBuilder.header(header);
                    }
                }

                currentLine = new StringBuilder();
            }
            default -> currentLine.append(character);
        }
    }

    public Request build(InputStream body) {
        Request request = requestBuilder.build();

        if (
                !request.method().equals(Method.GET) &&
                !request.method().equals(Method.HEAD) &&
                (!request.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue()) && !request.headers().containsKey(HeaderKey.TRANSFER_ENCODING.getValue()))
        ) {
            throw new ResponseStatusException("Headers 'Content-Length' or 'Transfer-Encoding' are mandatory.", Status.BAD_REQUEST);
        }

        init();

        return request;
    }

    private void init() {
        parsingState = ParsingState.REQUEST_LINE;
        currentLine = new StringBuilder();
        requestBuilder = new Request.Builder();
        isLineFeed = false;
    }
}
