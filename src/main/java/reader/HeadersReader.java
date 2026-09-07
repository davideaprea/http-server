package reader;

import common.TimedOperation;
import model.RequestBody;
import client.channel.ClientRequestsQueue;
import model.Request;
import parser.HeaderParser;
import parser.dto.Header;

import java.util.*;

public class HeadersReader extends RequestReader {
    private final Request.RequestBuilder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final Map<String, List<String>> headers = new HashMap<>();

    private ReadingState readingState = ReadingState.NORMAL;

    protected HeadersReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, TimedOperation timedOperation, Request.RequestBuilder requestBuilder) {
        super(clientRequestsQueue, onReadingAvailable, timedOperation);
        this.requestBuilder = requestBuilder;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new IllegalStateException();
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new IllegalStateException();
                }

                if (currentLine.isEmpty()) {
                    RequestBody requestBody = new RequestBody(onReadingAvailable);
                    Request request = requestBuilder
                            .headers(headers)
                            .body(requestBody)
                            .build();
                    Optional<Long> contentLengthValue = request.getContentLength();
                    Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

                    if (contentLengthValue.isPresent() && transferEncodingValue.isPresent()) {
                        throw new IllegalStateException("Both content length and transfer encoding headers are present.");
                    }

                    RequestReader nextReader;

                    if (contentLengthValue.filter(v -> v > 0).isPresent()) {
                        nextReader = new ContentLengthBodyReader(
                                clientRequestsQueue,
                                onReadingAvailable,
                                timedOperation,
                                requestBody,
                                contentLengthValue.get()
                        );
                    } else if (transferEncodingValue.isPresent()) {
                        nextReader = new ChunkedBodyReader(clientRequestsQueue, onReadingAvailable, timedOperation, requestBody);
                    } else {
                        requestBody.enqueue(-1);

                        nextReader = new RequestLineReader(clientRequestsQueue, onReadingAvailable, timedOperation);
                    }

                    clientRequestsQueue.enqueue(request);

                    return new ReadResult(
                            nextReader,
                            ReadResult.NextAction.PROCEED
                    );
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    headers.computeIfAbsent(header.name(), k -> new ArrayList<>()).add(header.value());
                    currentLine.setLength(0);
                }

                readingState = ReadingState.NORMAL;
            }
            default -> currentLine.append(c);
        }

        return new ReadResult(
                this,
                ReadResult.NextAction.PROCEED
        );
    }
}
