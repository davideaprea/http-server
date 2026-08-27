package reader;

import common.queue.RequestQueue;
import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.ContentLengthRequest;
import common.exception.ResponseStatusException;
import common.model.Request;
import common.model.Status;
import common.queue.RequestBodyBytesQueue;

import java.util.Optional;

public class HeadersReader extends RequestReader {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();

    private ReadingState readingState = ReadingState.NORMAL;


    public HeadersReader(Request.Builder requestBuilder, RequestQueue requestQueue) {
        super(requestQueue);

        this.requestBuilder = requestBuilder;
    }

    @Override
    public RequestReader eval(byte requestByte) {
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
                    RequestBodyBytesQueue requestBodyBytesQueue = new RequestBodyBytesQueue();
                    Request request = requestBuilder.body(requestBodyBytesQueue).build();

                    requestQueue.enqueue(request);

                    Optional<Long> contentLengthValue = request.getContentLength();
                    Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

                    if (contentLengthValue.isPresent() && transferEncodingValue.isPresent()) {
                        throw new ResponseStatusException("", Status.BAD_REQUEST);
                    }

                    if (contentLengthValue.isPresent()) {
                        return new ContentLengthBodyReader(new ContentLengthRequest(
                                requestBodyBytesQueue,
                                contentLengthValue.get()
                        ), requestQueue);
                    }

                    if (transferEncodingValue.isPresent()) {
                        return new ChunkedBodyReader(requestBodyBytesQueue, requestQueue);
                    }

                    requestBodyBytesQueue.enqueue(-1);

                    return new RequestLineReader(requestQueue);
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    requestBuilder.header(header);
                    currentLine.setLength(0);
                }

                readingState = ReadingState.NORMAL;
            }
            default -> currentLine.append(c);
        }

        return this;
    }
}
