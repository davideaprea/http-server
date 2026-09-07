package reader;

import model.RequestBody;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    protected ContentLengthBodyReader(ReadingLifecycleEvents readingLifecycleEvents, RequestBody requestBody, long remainingBytes) {
        super(readingLifecycleEvents);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;
    }


    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(readingLifecycleEvents);

            reader.eval(requestByte);

            return new ReadResult(reader, ReadResult.NextAction.PROCEED);
        }

        requestBody.enqueue(requestByte);

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.enqueue((byte) -1);
            readingLifecycleEvents.onEnd().run();

            return new ReadResult(new RequestLineReader(readingLifecycleEvents), ReadResult.NextAction.PROCEED);
        }

        return new ReadResult(this, requestBody.isFull() ? ReadResult.NextAction.WAIT : ReadResult.NextAction.PROCEED);
    }
}
