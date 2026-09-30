package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.client.dto.EnqueuedResponse;
import io.github.davideaprea.httpserver.model.*;
import io.github.davideaprea.httpserver.parser.HeaderParser;
import io.github.davideaprea.httpserver.parser.dto.Header;
import io.github.davideaprea.httpserver.parser.exception.BadFormatException;
import io.github.davideaprea.httpserver.reader.dto.Context;
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

    public HeadersReader(Context context, Request.RequestBuilder requestBuilder, long availableSpace) {
        super(context);
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
    public RequestReader evalNextReader(byte requestByte) {
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
                    RequestBody requestBody = new RequestBody(context.channelKey()::addReadInterest);
                    Request request = requestBuilder
                            .headers(headers)
                            .body(requestBody)
                            .build();
                    RequestReader nextReader;

                    if (request.getContentLength().filter(v -> v > 0).isPresent()) {
                        nextReader = new ContentLengthBodyReader(
                                context,
                                requestBody,
                                request.getContentLength().get()
                        );
                    } else if (request.getHeaderValue(HeaderKey.TRANSFER_ENCODING.getValue()).isPresent()) {
                        nextReader = new ChunkedBodyReader(context, requestBody);
                    } else {
                        requestBody.close();
                        context.requestTimer().stop();

                        nextReader = new RequestLineReader(context);
                    }

                    context.responsesQueue().enqueue(new EnqueuedResponse(
                            () -> {
                                Response response = context.router().handle(request);

                                if (request.isClosingRequest()) {
                                    response.headers().put(HeaderKey.CONNECTION.getValue(), "close");
                                }

                                return response;
                            },
                            Method.HEAD.equals(request.getMethod())
                    ));

                    return nextReader;
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

        return this;
    }
}
