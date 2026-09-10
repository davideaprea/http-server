package reader.lifecycle;

import model.RequestBody;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    public ContentLengthBodyReader(ReadingLifecycleEvents readingLifecycleEvents, RequestBody requestBody, long remainingBytes) {
        super(readingLifecycleEvents);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;
    }


    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(readingLifecycleEvents);

            reader.eval(requestByte);

            return new ReadResult(reader, true);
        }

        requestBody.enqueue(Byte.toUnsignedInt(requestByte));

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.close();
            readingLifecycleEvents.onEnd().run();

            return new ReadResult(new RequestLineReader(readingLifecycleEvents), true);
        }

        return new ReadResult(this, !requestBody.isFull());
    }
}
