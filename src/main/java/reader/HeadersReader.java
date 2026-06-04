package reader;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.RequestContext;
import reader.util.BodyReadingModeSelector;
import reader.util.CRLFSequenceStateTracker;
import shared.RequestBodyStream;
import shared.model.Request;

import java.util.concurrent.CompletableFuture;

public class HeadersReader extends ReadingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();
    private final BodyReadingModeSelector bodyReadingModeSelector;

    public HeadersReader(Request.Builder requestBuilder, RequestContext context) {
        super(context);

        this.requestBuilder = requestBuilder;
        this.bodyReadingModeSelector = new BodyReadingModeSelector(context);
    }

    @Override
    public ReadingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> CRLFSequenceStateTracker.setLineFeed();
            case '\r' -> {
                CRLFSequenceStateTracker.setCarriageReturn();

                if (currentLine.isEmpty()) {
                    Request request = requestBuilder.body(new RequestBodyStream()).build();

                    CompletableFuture.supplyAsync(
                            () -> context.router().handle(request),
                            context.executorService()
                    ).thenAccept(response -> {
                    });

                    return bodyReadingModeSelector.evalFromRequest(request);
                } else {
                    Header header = HeaderParser.from(currentLine.toString());

                    requestBuilder.header(header);
                    currentLine.setLength(0);
                }
            }
            default -> currentLine.append(requestByte);
        }

        return this;
    }
}
