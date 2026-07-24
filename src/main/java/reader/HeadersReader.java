package reader;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.RequestContext;
import reader.util.BodyReadingModeSelector;
import reader.util.CRLFSequenceStateTracker;
import shared.model.Request;
import shared.streaming.RequestBodyBytesQueue;

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
                    Request request = requestBuilder.body(new RequestBodyBytesQueue()).build();

                    CompletableFuture.supplyAsync(
                            () -> context.router().handle(request),
                            context.executorService()
                    ).thenAccept(response -> {
                    });

                    return BodyReadingModeSelector.evalFromRequest(request, context);
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
