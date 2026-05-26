package parser.state;

import shared.model.Request;

public class TransferEncodingBodyState implements ParsingState {
    private boolean isReadingChunkSize = true;
    private StringBuilder currentChunkSizeInHex = new StringBuilder();
    private boolean isLineFeed;
    private long chunkSize = 0;
    private final Request request;

    public TransferEncodingBodyState(Request request) {
        this.request = request;
    }

    @Override
    public ParsingState eval(byte requestByte) {
        if (isReadingChunkSize) {
            if (requestByte == '\n') {
                if (isLineFeed) {
                    //throw
                }

                isLineFeed = true;
            } else if (requestByte == '\r') {
                if (!isLineFeed) {
                    //throw
                }

                isLineFeed = false;
                isReadingChunkSize = false;
                chunkSize = Long.parseLong(currentChunkSizeInHex.toString(), 16);
                currentChunkSizeInHex.setLength(0);
            } else {
                currentChunkSizeInHex.append(requestByte);
            }
        } else {
            chunkSize--;

            if (chunkSize == 0) {
                isReadingChunkSize = true;
            }
        }

        return this;
    }
}
