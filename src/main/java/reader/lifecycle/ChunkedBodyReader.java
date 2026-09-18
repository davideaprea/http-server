package reader.lifecycle;

import model.RequestBody;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

/**
 * Reads the body of an HTTP request encoded using chunked transfer encoding.
 */
public class ChunkedBodyReader extends RequestReader {
    private static final int CHUNK_SIZE_MAX_LENGTH = 16;

    private boolean isReadingChunkSize = true;
    private StringBuilder chunkSizeBuilder = new StringBuilder();
    private long remainingChunkBytes = 0;
    private long currentChunkBytes = 0;
    private ReadingState readingState = ReadingState.NORMAL;
    private long availableSpace;

    private final RequestBody requestBody;

    public ChunkedBodyReader(ReadingLifecycleEvents readingLifecycleEvents, RequestBody requestBody, SizeLimits sizeLimits) {
        super(readingLifecycleEvents, sizeLimits);
        this.requestBody = requestBody;
        availableSpace = sizeLimits.maxBodySize();
    }

    /**
     * <p>The byte is processed as part of the current chunk size, chunk data,
     * or chunk delimiter. Once the terminating chunk is received, the request
     * body is closed and the reading lifecycle proceeds to the next request.</p>
     */
    @Override
    public ReadResult eval(byte requestByte) {
        if (isReadingChunkSize) {
            char currChar = (char) requestByte;

            if (currChar == '\r') {
                if (!ReadingState.NORMAL.equals(readingState)) {
                    throw new IllegalStateException("Invalid CRLF sequence in body chunk.");
                }

                readingState = ReadingState.CARRIAGE_RETURN;
            } else if (currChar == '\n') {
                if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                    throw new IllegalStateException("Invalid CRLF sequence in body chunk.");
                }

                try {
                    currentChunkBytes = Long.parseLong(chunkSizeBuilder.toString(), 16);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Chunk size is not a valid number.");
                }

                remainingChunkBytes = currentChunkBytes;
                readingState = ReadingState.NORMAL;
                isReadingChunkSize = false;
                chunkSizeBuilder = new StringBuilder();
            } else {
                if (chunkSizeBuilder.length() == CHUNK_SIZE_MAX_LENGTH) {
                    throw new IllegalStateException("Max body size exceeded");
                }

                chunkSizeBuilder.append(currChar);
            }
        } else {
            if (remainingChunkBytes > 0) {
                if (availableSpace == 0) {
                    throw new IllegalStateException("Max body size exceeded");
                }

                availableSpace--;
                requestBody.enqueue(Byte.toUnsignedInt(requestByte));
                remainingChunkBytes--;
            } else {
                if ((char) requestByte == '\r') {
                    if (!ReadingState.NORMAL.equals(readingState)) {
                        throw new IllegalStateException("Invalid CRLF sequence in body chunk.");
                    }

                    readingState = ReadingState.CARRIAGE_RETURN;
                } else if ((char) requestByte == '\n') {
                    if (!ReadingState.CARRIAGE_RETURN.equals(readingState)) {
                        throw new IllegalStateException("Invalid CRLF sequence in body chunk.");
                    }

                    readingState = ReadingState.NORMAL;
                    isReadingChunkSize = true;

                    if (currentChunkBytes == 0) {
                        requestBody.close();
                        readingLifecycleEvents.onEnd().run();

                        return new ReadResult(
                                new RequestLineReader(readingLifecycleEvents, sizeLimits),
                                true
                        );
                    }
                } else {
                    throw new IllegalStateException("Invalid character found in body chunk.");
                }
            }
        }

        return new ReadResult(this, !requestBody.isFull());
    }
}
