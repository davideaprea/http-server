package reader.state;

import parser.HeaderParser;
import parser.dto.Header;
import reader.dto.RequestContext;
import shared.RequestBodyStream;
import shared.model.Request;

import java.util.concurrent.CompletableFuture;

public class HeadersState extends ParsingState {
    private final Request.Builder requestBuilder;
    private final StringBuilder currentLine = new StringBuilder();
    private final CRLFSequenceState CRLFSequenceState = new CRLFSequenceState();

    public HeadersState(Request.Builder requestBuilder, RequestContext context) {
        super(context);

        this.requestBuilder = requestBuilder;
    }

    @Override
    public ParsingState eval(byte requestByte) {
        switch (requestByte) {
            case '\n' -> CRLFSequenceState.setLineFeed();
            case '\r' -> {
                CRLFSequenceState.setCarriageReturn();

                if (currentLine.isEmpty()) {
                    Request request = requestBuilder.body(new RequestBodyStream()).build();

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
            default -> currentLine.append(requestByte);
        }

        return this;
    }
}
