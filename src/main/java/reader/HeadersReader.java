package reader;

import common.util.MultiValueMap;
import common.exception.ResponseStatusException;
import common.queue.RequestBodyBytesQueue;
import client.ClientRequestsQueue;
import model.Request;
import model.Status;
import parser.HeaderParser;
import parser.dto.Header;

import java.util.Optional;

public class HeadersReader extends RequestReader {
    private final Request.RequestBuilder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final MultiValueMap<String, String> headers = new MultiValueMap<>();

    private ReadingState readingState = ReadingState.NORMAL;

    protected HeadersReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, Request.RequestBuilder requestBuilder) {
        super(clientRequestsQueue, onReadingAvailable);
        this.requestBuilder = requestBuilder;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\r' -> {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            }
            case '\n' -> {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                if (currentLine.isEmpty()) {
                    RequestBodyBytesQueue requestBodyBytesQueue = new RequestBodyBytesQueue(onReadingAvailable);
                    Request request = requestBuilder
                            .headers(headers)
                            .body(requestBodyBytesQueue)
                            .build();
                    Optional<Long> contentLengthValue = request.getContentLength();
                    Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

                    if (contentLengthValue.isPresent() && transferEncodingValue.isPresent()) {
                        throw new ResponseStatusException("", Status.BAD_REQUEST);
                    }

                    RequestReader nextReader;

                    if (contentLengthValue.filter(v -> v > 0).isPresent()) {
                        nextReader = new ContentLengthBodyReader(
                                clientRequestsQueue,
                                onReadingAvailable,
                                requestBodyBytesQueue,
                                contentLengthValue.get()
                        );
                    } else if (transferEncodingValue.isPresent()) {
                        nextReader = new ChunkedBodyReader(clientRequestsQueue, onReadingAvailable, requestBodyBytesQueue);
                    } else {
                        requestBodyBytesQueue.enqueue(-1);

                        nextReader = new RequestLineReader(clientRequestsQueue, onReadingAvailable);
                    }

                    clientRequestsQueue.enqueue(request);

                    return new ReadResult(
                            nextReader,
                            ReadResult.NextAction.PROCEED
                    );
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    headers.add(header.name(), header.value());
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
