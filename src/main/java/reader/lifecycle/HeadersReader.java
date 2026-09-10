package reader.lifecycle;

import common.MalformedRequestException;
import model.HeaderKey;
import model.Request;
import model.RequestBody;
import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

import java.util.*;

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

    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

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
                    Optional<Long> contentLengthValue = Optional.ofNullable(headers.get(HeaderKey.CONTENT_LENGTH.getValue()))
                            .filter(values -> !values.isEmpty())
                            .map(values -> {
                                if (values.size() > 1) {
                                    throw new MalformedRequestException("Content length header allows only one value.");
                                }

                                long value = Long.parseLong(values.getFirst());

                                if (value < 0) {
                                    throw new MalformedRequestException("Content length header allows only zero or positive values.");
                                }

                                return value;
                            });
                    Optional<String> transferEncodingValue = Optional
                            .ofNullable(headers.get(HeaderKey.TRANSFER_ENCODING.getValue()))
                            .map(List::getFirst);

                    if (contentLengthValue.isPresent() && transferEncodingValue.isPresent()) {
                        throw new MalformedRequestException("Content length and transfer encoding headers can't be present in the same request.");
                    }

                    RequestReader nextReader;

                    if (contentLengthValue.filter(v -> v > 0).isPresent()) {
                        nextReader = new ContentLengthBodyReader(
                                readingLifecycleEvents,
                                requestBody,
                                contentLengthValue.get(),
                                sizeLimits
                        );
                    } else if (transferEncodingValue.isPresent()) {
                        nextReader = new ChunkedBodyReader(readingLifecycleEvents, requestBody, sizeLimits);
                    } else {
                        requestBody.close();

                        nextReader = new RequestLineReader(readingLifecycleEvents, sizeLimits);

                        readingLifecycleEvents.onEnd().run();
                    }

                    readingLifecycleEvents.onNewRequest().accept(request);

                    return new ReadResult(
                            nextReader,
                            true
                    );
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

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
