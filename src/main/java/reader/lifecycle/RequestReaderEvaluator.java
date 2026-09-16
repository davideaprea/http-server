package reader.lifecycle;

import reader.dto.ReadResult;
import reader.dto.ReadingError;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;
import reader.exception.MalformedRequestException;

public class RequestReaderEvaluator {
    private final ReadingLifecycleEvents readingLifecycleEvents;
    private final SizeLimits sizeLimits;

    private RequestReader requestReader;

    public RequestReaderEvaluator(ReadingLifecycleEvents readingLifecycleEvents, SizeLimits sizeLimits) {
        this.readingLifecycleEvents = readingLifecycleEvents;
        this.sizeLimits = sizeLimits;

        reset();
    }

    public boolean eval(byte requestByte) {
        try {
            ReadResult result = requestReader.eval(requestByte);
            requestReader = result.nextReader();

            return result.canProceed();
        } catch (Exception e) {
            readingLifecycleEvents.onError().accept(new ReadingError(e, e instanceof MalformedRequestException));

            reset();

            return true;
        }
    }

    public void reset() {
        requestReader = new RequestLineReader(readingLifecycleEvents, sizeLimits);
    }
}
