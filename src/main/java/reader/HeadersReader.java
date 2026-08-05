package reader;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.ContentLengthRequest;
import reader.util.CRLFSequenceStateTracker;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;
import shared.streaming.RequestBodyBytesQueue;
import shared.streaming.RequestQueue;

import java.util.Optional;

public class HeadersReader extends ReadingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();

    public HeadersReader(Request.Builder requestBuilder, RequestQueue requestQueue) {
        super(requestQueue);

        this.requestBuilder = requestBuilder;
    }

    @Override
    public ReadingState eval(byte requestByte) {
        char c = (char) requestByte;

        switch (c) {
            case '\n' -> CRLFSequenceStateTracker.setLineFeed();
            case '\r' -> {
                CRLFSequenceStateTracker.setCarriageReturn();

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

                    return new RequestLineReader(requestQueue);
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    requestBuilder.header(header);
                    currentLine.setLength(0);
                }
            }
            default -> currentLine.append(c);
        }

        return this;
    }
}
