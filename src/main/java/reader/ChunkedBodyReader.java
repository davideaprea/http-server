package reader;

import reader.dto.RequestContext;
import reader.util.CRLFSequenceStateTracker;
import shared.exception.ResponseStatusException;
import shared.model.Status;
import shared.streaming.BodyBytesEnqueue;

public class ChunkedBodyReader extends ReadingState {
    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;

    private final BodyBytesEnqueue bodyStream;
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();

    public ChunkedBodyReader(BodyBytesEnqueue bodyStream, RequestContext context) {
        super(context);

        this.bodyStream = bodyStream;
    }

    @Override
    public ReadingState eval(byte requestByte) {
        if (isReadingChunkSize) {
            if (requestByte == '\n') {
                CRLFSequenceStateTracker.setLineFeed();
            } else if (requestByte == '\r') {
                CRLFSequenceStateTracker.setCarriageReturn();

                isReadingChunkSize = false;
                currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                remainingChunkBytes = currentChunkBytes;
                chunkSizeBuilder = new StringBuilder();
            } else {
                chunkSizeBuilder.append(requestByte);
            }
        } else {
            if (remainingChunkBytes > 0) {
                bodyStream.enqueue(requestByte);
                remainingChunkBytes--;
            } else {
                if (requestByte == '\n') {
                    CRLFSequenceStateTracker.setLineFeed();
                } else if (requestByte == '\r') {
                    CRLFSequenceStateTracker.setCarriageReturn();

                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        bodyStream.enqueue((byte) -1);

                        return new RequestLineReader(context);
                    }
                } else {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }
            }
        }

        return this;
    }
}
