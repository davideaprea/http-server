package reader;

import common.exception.ResponseStatusException;
import model.RequestBody;
import client.ClientRequestsQueue;
import model.Status;

public class ChunkedBodyReader extends RequestReader {
    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;
    private ReadingState readingState = ReadingState.NORMAL;

    private final RequestBody requestBody;

    protected ChunkedBodyReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, RequestBody requestBody) {
        super(clientRequestsQueue, onReadingAvailable);
        this.requestBody = requestBody;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        if (isReadingChunkSize) {
            char currChar = (char) requestByte;

            if (currChar == '\r') {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            } else if (currChar == '\n') {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new ResponseStatusException(Status.BAD_REQUEST);
                }

                readingState = ReadingState.NORMAL;
                isReadingChunkSize = false;
                currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                remainingChunkBytes = currentChunkBytes;
                chunkSizeBuilder = new StringBuilder();
            } else {
                chunkSizeBuilder.append(currChar);
            }
        } else {
            if (remainingChunkBytes > 0) {
                requestBody.enqueue(requestByte);
                remainingChunkBytes--;
            } else {
                if ((char) requestByte == '\r') {
                    if (!ReadingState.NORMAL.equals(readingState)) {
                        throw new ResponseStatusException(Status.BAD_REQUEST);
                    }

                    readingState = ReadingState.CARRIAGE_RETURN;
                } else if ((char) requestByte == '\n') {
                    if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                        throw new ResponseStatusException(Status.BAD_REQUEST);
                    }

                    readingState = ReadingState.NORMAL;
                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        requestBody.enqueue((byte) -1);

                        return new ReadResult(
                                new RequestLineReader(clientRequestsQueue, onReadingAvailable),
                                ReadResult.NextAction.PROCEED
                        );
                    }
                } else {
                    throw new ResponseStatusException("Malformed request.", Status.BAD_REQUEST);
                }
            }
        }

        return new ReadResult(this, requestBody.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
