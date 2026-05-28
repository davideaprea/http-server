package parser.state;

import parser.dto.RequestContext;
import shared.RequestBodyStream;
import shared.exception.ResponseStatusException;
import shared.model.Status;

public class ChunkedBodyState extends ParsingState {
    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;

    private final RequestBodyStream bodyStream;
    private final CRLFSequenceValidator CRLFSequenceValidator = new CRLFSequenceValidator();

    public ChunkedBodyState(RequestBodyStream bodyStream, RequestContext context) {
        super(context);

        this.bodyStream = bodyStream;
    }

    @Override
    public ParsingState eval(byte requestByte) {
        if (isReadingChunkSize) {
            if (requestByte == '\n') {
                CRLFSequenceValidator.setLineFeedState();
            } else if (requestByte == '\r') {
                CRLFSequenceValidator.setCarriageReturnState();

                isReadingChunkSize = false;
                currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                remainingChunkBytes = currentChunkBytes;
                chunkSizeBuilder = new StringBuilder();
            } else {
                chunkSizeBuilder.append(requestByte);
            }
        } else {
            if (remainingChunkBytes > 0) {
                bodyStream.append(requestByte);
                remainingChunkBytes--;
            } else {
                if (requestByte == '\n') {
                    CRLFSequenceValidator.setLineFeedState();
                } else if (requestByte == '\r') {
                    CRLFSequenceValidator.setCarriageReturnState();

                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        return new RequestLineState(context);
                    }
                } else {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }
            }
        }

        return this;
    }
}
