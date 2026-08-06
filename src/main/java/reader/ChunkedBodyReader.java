package reader;

import reader.util.CRLFSequenceStateTracker;
import common.exception.ResponseStatusException;
import common.model.Status;
import common.streaming.BodyBytesEnqueue;

public class ChunkedBodyReader extends RequestReader {
    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;

    private final BodyBytesEnqueue bodyStream;
    private final CRLFSequenceStateTracker CRLFSequenceStateTracker = new CRLFSequenceStateTracker();

    public ChunkedBodyReader(BodyBytesEnqueue bodyStream, RequestQueue requestQueue) {
        super(requestQueue);

        this.bodyStream = bodyStream;
    }

    @Override
    public RequestReader eval(byte requestByte) {
        if (isReadingChunkSize) {
            char currChar = (char) requestByte;

            if (currChar == '\r') {
                CRLFSequenceStateTracker.setCarriageReturn();
            } else if (currChar == '\n') {
                CRLFSequenceStateTracker.setLineFeed();

                isReadingChunkSize = false;
                currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                remainingChunkBytes = currentChunkBytes;
                chunkSizeBuilder = new StringBuilder();
            } else {
                chunkSizeBuilder.append(currChar);
            }
        } else {
            if (remainingChunkBytes > 0) {
                bodyStream.enqueue(requestByte);
                remainingChunkBytes--;
            } else {
                if ((char) requestByte == '\r') {
                    CRLFSequenceStateTracker.setCarriageReturn();
                } else if ((char) requestByte == '\n') {
                    CRLFSequenceStateTracker.setLineFeed();

                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        bodyStream.enqueue((byte) -1);

                        return new RequestLineReader(requestQueue);
                    }
                } else {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }
            }
        }

        return this;
    }
}
