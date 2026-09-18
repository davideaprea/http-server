package reader.lifecycle;

import model.Request;
import parser.RequestTargetParser;
import parser.dto.RequestTarget;
import parser.exception.BadFormatException;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;
import reader.exception.MalformedRequestException;

/**
 * Reads and parses the request line of an HTTP request.
 */
public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

    private ReadingState readingState = ReadingState.NORMAL;
    private long availableSpace;

    public RequestLineReader(ReadingLifecycleEvents readingLifecycleEvents, SizeLimits sizeLimits) {
        super(readingLifecycleEvents, sizeLimits);

        availableSpace = sizeLimits.maxHeadersSize();

        readingLifecycleEvents.onStart().run();
    }

    /**
     * Processes a byte of the request line.
     *
     * <p>Once the request line is complete, the parsed request information is
     * passed to the next stage of the request reading lifecycle.</p>
     *
     * @throws MalformedRequestException if the request line has an invalid
     *                                   format or exceeds the configured {@link SizeLimits#maxHeadersSize()}
     */
    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in request line.");
                }

                RequestTarget requestTarget;

                try {
                    requestTarget = RequestTargetParser.from(requestLineBuilder.toString());
                } catch (BadFormatException e) {
                    throw new MalformedRequestException(e.getMessage());
                }

                requestBuilder
                        .method(requestTarget.method())
                        .version(requestTarget.version())
                        .url(requestTarget.url())
                        .queryParams(requestTarget.queryParams());

                readingState = ReadingState.NORMAL;

                return new ReadResult(
                        new HeadersReader(readingLifecycleEvents, requestBuilder, sizeLimits, availableSpace),
                        true
                );
            }
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in request line.");
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            default -> {
                if (availableSpace == 0) {
                    throw new MalformedRequestException("Request line and headers exceeded max size.");
                }

                availableSpace--;
                requestLineBuilder.append(c);
            }
        }

        return new ReadResult(
                this,
                true
        );
    }
}
