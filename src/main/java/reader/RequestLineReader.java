package reader;

import common.exception.ResponseStatusException;
import common.model.Request;
import common.model.Status;
import parser.RequestLineParser;
import parser.RequestTargetParser;
import parser.dto.RequestLine;

public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.Builder requestBuilder = new Request.Builder();

    private ReadingState readingState = ReadingState.NORMAL;

    public RequestLineReader(RequestQueue requestQueue) {
        super(requestQueue);
    }

    @Override
    public RequestReader eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                RequestLine requestLine = RequestLineParser.from(requestLineBuilder.toString());

                requestBuilder
                        .method(requestLine.method())
                        .version(requestLine.version())
                        .requestTarget(RequestTargetParser.from(requestLine.requestTarget()));

                readingState = ReadingState.NORMAL;

                return new HeadersReader(requestBuilder, requestQueue);
            }
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            default -> requestLineBuilder.append(c);
        }

        return this;
    }
}
