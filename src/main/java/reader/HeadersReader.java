package reader;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;
import reader.util.CRLFSequenceStateTracker;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;
import shared.streaming.RequestBodyBytesQueue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HeadersReader extends ReadingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();

    public HeadersReader(Request.Builder requestBuilder, RequestContext context) {
        super(context);

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

                    CompletableFuture.supplyAsync(
                            () -> context.router().handle(request),
                            context.executorService()
                    ).thenAccept(response -> {
                    });

                    Optional<Long> contentLengthValue = request.getContentLength();
                    Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

                    if (contentLengthValue.isEmpty() && transferEncodingValue.isEmpty()) {
                        throw new ResponseStatusException("", Status.BAD_REQUEST);
                    }

                    if (contentLengthValue.isPresent()) {
                        return new ContentLengthBodyReader(new ContentLengthRequest(
                                requestBodyBytesQueue,
                                contentLengthValue.get()
                        ), context);
                    }

                    return new ChunkedBodyReader(requestBodyBytesQueue, context);
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
