package parser.state;

import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

public class ChunkedBodyState implements ParsingState {
    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private boolean isLineFeed = false;
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;
    private final Request request;

    public ChunkedBodyState(Request request) {
        this.request = request;
    }

    @Override
    public ParsingState eval(byte requestByte) {
        if (isReadingChunkSize) {
            if (requestByte == '\n') {
                if (isLineFeed) {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }

                isLineFeed = true;
            } else if (requestByte == '\r') {
                if (!isLineFeed) {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }

                isLineFeed = false;
                isReadingChunkSize = false;
                currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                remainingChunkBytes = currentChunkBytes;
                chunkSizeBuilder = new StringBuilder();
            } else {
                chunkSizeBuilder.append(requestByte);
            }
        } else {
            if (remainingChunkBytes > 0) {
                request.body().append(requestByte);
                remainingChunkBytes--;
            } else {
                if (requestByte == '\n') {
                    if (isLineFeed) {
                        throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                    }

                    isLineFeed = true;
                } else if (requestByte == '\r') {
                    if (!isLineFeed) {
                        throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                    }

                    isLineFeed = false;
                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        return new RequestLineState();
                    }
                } else {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }
            }
        }

        return this;
    }
}
