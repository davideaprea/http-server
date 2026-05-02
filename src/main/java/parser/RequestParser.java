package parser;

import parser.dto.Header;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParser {
    private ParsingState parsingState;
    private boolean isLineFeed;
    StringBuilder currentLine;

    Method method;
    Version version;
    RequestTarget requestTarget;
    Map<String, List<String>> headers = new HashMap<>();

    public RequestParser() {
        init();
    }

    public void eval(char character) {
        if (character == '\n') {
            if (isLineFeed) {
                //throw
            }

            isLineFeed = true;
        } else if (character == '\r') {
            if (!isLineFeed) {
                //throw
            }

            switch (parsingState) {
                case REQUEST_LINE -> {
                    RequestLine requestLine = RequestLineParser.from(currentLine.toString());
                    method = requestLine.method();
                    version = requestLine.version();
                    requestTarget = RequestTargetParser.from(requestLine.requestTarget());
                    parsingState = ParsingState.HEADER;
                }
                case HEADER -> {
                    Header header = HeaderParser.from(currentLine.toString());

                    headers.putIfAbsent(header.name(), new ArrayList<>());
                    headers.get(header.name()).add(header.value());
                }
            }

            currentLine = new StringBuilder();
            isLineFeed = false;
        } else {
            currentLine.append(character);
        }
    }

    public Request build(InputStream body) {
        if (
                !method.equals(Method.GET) &&
                        !method.equals(Method.HEAD) &&
                        !headers.containsKey(HeaderKey.CONTENT_LENGTH.getValue())
        ) {
            throw new ResponseStatusException("Headers 'Content-Length' or 'Transfer-Encoding' are mandatory.", Status.BAD_REQUEST);
        }

        Request request = new Request(
                method,
                version,
                requestTarget,
                headers,
                body
        );

        init();

        return request;
    }

    private void init() {
        parsingState = ParsingState.REQUEST_LINE;
        isLineFeed = false;
        currentLine = new StringBuilder();
    }
}
