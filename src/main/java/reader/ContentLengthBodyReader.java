package reader;

import model.RequestBody;
import client.channel.ClientRequestsQueue;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    protected ContentLengthBodyReader(ClientRequestsQueue clientRequestsQueue, Runnable onReadingAvailable, RequestBody requestBody, long remainingBytes) {
        super(clientRequestsQueue, onReadingAvailable);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;
    }

    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(clientRequestsQueue, onReadingAvailable);

            reader.eval(requestByte);

            return new ReadResult(reader, ReadResult.NextAction.PROCEED);
        }

        requestBody.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.enqueue((byte) -1);

            return new ReadResult(new RequestLineReader(clientRequestsQueue, onReadingAvailable), ReadResult.NextAction.PROCEED);
        }

        return new ReadResult(this, requestBody.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
