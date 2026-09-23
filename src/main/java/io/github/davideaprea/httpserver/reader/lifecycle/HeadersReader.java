package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Request;
import io.github.davideaprea.httpserver.model.RequestBody;
import io.github.davideaprea.httpserver.parser.HeaderParser;
import io.github.davideaprea.httpserver.parser.dto.Header;
import io.github.davideaprea.httpserver.parser.exception.BadFormatException;
import io.github.davideaprea.httpserver.reader.dto.ReadResult;
import io.github.davideaprea.httpserver.reader.dto.ReadingLifecycleEvents;
import io.github.davideaprea.httpserver.reader.dto.SizeLimits;
import io.github.davideaprea.httpserver.reader.exception.MalformedRequestException;

import java.util.*;

/**
 * Reads and parses the headers of an HTTP request.
 */
public class HeadersReader extends RequestReader {
    private final Request.RequestBuilder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final Map<String, List<String>> headers = new HashMap<>();

    private ReadingState readingState = ReadingState.NORMAL;
    private long availableSpace;

    public HeadersReader(ReadingLifecycleEvents readingLifecycleEvents, Request.RequestBuilder requestBuilder, SizeLimits sizeLimits, long availableSpace) {
        super(readingLifecycleEvents, sizeLimits);
        this.requestBuilder = requestBuilder;
        this.availableSpace = availableSpace;
    }

    /**
     * Processes a byte of the request headers.
     *
     * <p>Once all headers have been read, the request body configuration is
     * determined and the request is passed to the appropriate stage of the
     * request reading lifecycle.</p>
     *
     * @throws MalformedRequestException if the headers have an invalid format,
     *                                   contain conflicting body information,
     *                                   or exceed the configured {@link SizeLimits#maxHeadersSize()}
     */
    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) (requestByte & 0xFF);

        switch (c) {
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in headers.");
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new MalformedRequestException("Invalid CRLF sequence in headers.");
                }

                if (currentLine.isEmpty()) {
                    RequestBody requestBody = new RequestBody(readingLifecycleEvents.onReadingAvailable());
                    Request request = requestBuilder
                            .headers(headers)
                            .body(requestBody)
                            .build();
                    RequestReader nextReader;

                    if (request.getContentLength().filter(v -> v > 0).isPresent()) {
                        nextReader = new ContentLengthBodyReader(
                                readingLifecycleEvents,
                                requestBody,
                                request.getContentLength().get(),
                                sizeLimits
                        );
                    } else if (request.getHeaderValue(HeaderKey.TRANSFER_ENCODING.getValue()).isPresent()) {
                        nextReader = new ChunkedBodyReader(readingLifecycleEvents, requestBody, sizeLimits);
                    } else {
                        requestBody.close();
                        readingLifecycleEvents.onEnd().run();

                        nextReader = new RequestLineReader(readingLifecycleEvents, sizeLimits);
                    }

                    readingLifecycleEvents.onNewRequest().accept(request);

                    return new ReadResult(
                            nextReader,
                            true
                    );
                } else {
                    Header header;

                    try {
                        header = HeaderParser.from(currentLine.toString());
                    } catch (BadFormatException e) {
                        throw new MalformedRequestException(e.getMessage());
                    }

                    headers.computeIfAbsent(header.name(), k -> new ArrayList<>()).add(header.value());
                    currentLine.setLength(0);
                }

                readingState = ReadingState.NORMAL;
            }
            default -> {
                if (availableSpace == 0) {
                    throw new MalformedRequestException("Request line and headers exceeded max size.");
                }

                availableSpace--;
                currentLine.append(c);
            }
        }

        return new ReadResult(
                this,
                true
        );
    }
}
