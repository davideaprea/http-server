package reader;

import common.exception.ResponseStatusException;
import common.queue.RequestQueue;
import model.Request;
import model.Status;
import parser.RequestTargetParser;
import parser.dto.RequestTarget;

public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

    private ReadingState readingState = ReadingState.NORMAL;

    public RequestLineReader(RequestQueue requestQueue, Runnable onReadingAvailable) {
        super(requestQueue, onReadingAvailable);
    }

    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                RequestTarget requestTarget = RequestTargetParser.from(requestLineBuilder.toString());

                requestBuilder
                        .method(requestTarget.method())
                        .version(requestTarget.version())
                        .url(requestTarget.url())
                        .queryParams(requestTarget.queryParams());

                readingState = ReadingState.NORMAL;

                return new ReadResult(
                        new HeadersReader(requestQueue, onReadingAvailable, requestBuilder),
                        ReadResult.NextAction.PROCEED
                );
            }
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            default -> requestLineBuilder.append(c);
        }

        return new ReadResult(
                this,
                ReadResult.NextAction.PROCEED
        );
    }
}
