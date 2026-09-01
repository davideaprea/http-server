package reader;

import common.exception.ResponseStatusException;
import parser.dto.RequestTarget;
import model.Request;
import model.Status;
import common.queue.RequestQueue;
import parser.RequestLineParser;
import parser.RequestTargetParser;
import parser.dto.RequestLine;

public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

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
                RequestTarget requestTarget = RequestTargetParser.from(requestLine.requestTarget());

                requestBuilder
                        .method(requestLine.method())
                        .version(requestLine.version())
                        .url(requestTarget.url())
                        .queryParams(requestTarget.queryParams());

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
