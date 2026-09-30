package io.github.davideaprea.httpserver.connection.lifecycle;

import io.github.davideaprea.httpserver.model.RequestBody;
import io.github.davideaprea.httpserver.connection.dto.Context;

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

    public ChunkedBodyReader(Context context, RequestBody requestBody) {
        super(context);
        this.requestBody = requestBody;
        availableSpace = context.sizeLimits().maxBodySize();
    }

    /**
     * <p>The byte is processed as part of the current chunk size, chunk data,
     * or chunk delimiter. Once the terminating chunk is received, the request
     * body is closed and the reading lifecycle proceeds to the next request.</p>
     */
    @Override
    public RequestReader evalNextReader(byte requestByte) {
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

                if (!isHexDigit(currChar)) {
                    throw new IllegalStateException("Character %s is not a valid hexadecimal digit.".formatted(currChar));
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
                        context.requestTimer().stop();

                        return new RequestLineReader(context);
                    }
                } else {
                    throw new IllegalStateException("Invalid character found in body chunk.");
                }
            }
        }

        return this;
    }

    @Override
    public boolean isFree() {
        return !requestBody.isFull();
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }
}
