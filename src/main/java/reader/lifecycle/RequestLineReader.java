package reader.lifecycle;

import common.MalformedRequestException;
import model.Request;
import parser.RequestTargetParser;
import parser.dto.RequestTarget;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;

public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

    private ReadingState readingState = ReadingState.NORMAL;

    public RequestLineReader(ReadingLifecycleEvents readingLifecycleEvents) {
        super(readingLifecycleEvents);

        readingLifecycleEvents.onStart().run();
    }


    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in request line.");
                }

                RequestTarget requestTarget = RequestTargetParser.from(requestLineBuilder.toString());

                requestBuilder
                        .method(requestTarget.method())
                        .version(requestTarget.version())
                        .url(requestTarget.url())
                        .queryParams(requestTarget.queryParams());

                readingState = ReadingState.NORMAL;

                return new ReadResult(
                        new HeadersReader(readingLifecycleEvents, requestBuilder),
                        true
                );
            }
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in request line.");
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            default -> requestLineBuilder.append(c);
        }

        return new ReadResult(
                this,
                true
        );
    }
}
