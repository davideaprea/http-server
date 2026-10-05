package io.github.davideaprea.httpserver.connection.lifecycle;

import io.github.davideaprea.httpserver.connection.channel.ClientChannel;
import io.github.davideaprea.httpserver.model.Request;
import io.github.davideaprea.httpserver.parser.RequestTargetParser;
import io.github.davideaprea.httpserver.parser.dto.RequestTarget;
import io.github.davideaprea.httpserver.parser.exception.BadFormatException;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.connection.exception.MalformedRequestException;

/**
 * Reads and parses the request line of an HTTP request.
 */
public class RequestLineReader extends RequestReader {
    private final StringBuilder requestLineBuilder = new StringBuilder();
    private final Request.RequestBuilder requestBuilder = Request.builder();

    private ReadingState readingState = ReadingState.NORMAL;
    private long availableSpace;

    public RequestLineReader(ClientChannel clientChannel) {
        super(clientChannel);

        availableSpace = clientChannel.getSizeLimits().maxHeadersSize();

        clientChannel.getRequestTimer().start(clientChannel::close);
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
    public RequestReader evalNextReader(byte requestByte) {
        char c = (char) (requestByte & 0xFF);

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

                return new HeadersReader(clientChannel, requestBuilder, availableSpace);
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

        return this;
    }
}
