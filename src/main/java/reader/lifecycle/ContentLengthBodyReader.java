package reader.lifecycle;

import model.RequestBody;
import reader.dto.ReadResult;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

public class ContentLengthBodyReader extends RequestReader {
    private final RequestBody requestBody;

    private long remainingBytes;

    public ContentLengthBodyReader(ReadingLifecycleEvents readingLifecycleEvents, RequestBody requestBody, long remainingBytes, SizeLimits sizeLimits) {
        super(readingLifecycleEvents, sizeLimits);
        this.requestBody = requestBody;
        this.remainingBytes = remainingBytes;

        if (remainingBytes > sizeLimits.maxBodySize()) {
            throw new IllegalStateException("Max body size exceeded");
        }
    }


    @Override
    public ReadResult eval(byte requestByte) {
        if (remainingBytes == 0) {
            RequestReader reader = new RequestLineReader(readingLifecycleEvents, sizeLimits);

            reader.eval(requestByte);

            return new ReadResult(reader, true);
        }

        requestBody.enqueue(Byte.toUnsignedInt(requestByte));

        remainingBytes--;

        if (remainingBytes == 0) {
            requestBody.close();
            readingLifecycleEvents.onEnd().run();

            return new ReadResult(new RequestLineReader(readingLifecycleEvents, sizeLimits), true);
        }

        return new ReadResult(this, !requestBody.isFull());
    }
}
