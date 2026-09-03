package reader;

import client.ClientRequestsQueue;
import model.Request;
import parser.RequestTargetParser;
import parser.dto.RequestTarget;

public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

    private ReadingState readingState = ReadingState.NORMAL;

    public RequestLineReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable) {
        super(clientRequestsQueue, onReadingAvailable);
    }

    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new IllegalStateException();
                }

                RequestTarget requestTarget = RequestTargetParser.from(requestLineBuilder.toString());

                requestBuilder
                        .method(requestTarget.method())
                        .version(requestTarget.version())
                        .url(requestTarget.url())
                        .queryParams(requestTarget.queryParams());

                readingState = ReadingState.NORMAL;

                return new ReadResult(
                        new HeadersReader(clientRequestsQueue, onReadingAvailable, requestBuilder),
                        ReadResult.NextAction.PROCEED
                );
            }
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new IllegalStateException();
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
