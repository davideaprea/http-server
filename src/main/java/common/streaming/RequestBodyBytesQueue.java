package common.streaming;

import common.exception.ResponseStatusException;
import common.model.Status;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestBodyBytesQueue implements BodyBytesDequeue, BodyBytesEnqueue {
    private final BlockingQueue<Byte> bufferedBytes = new LinkedBlockingQueue<>();

    private boolean isClosed = false;

    public void enqueue(byte bodyByte) {
        if (isClosed) {
            throw new IllegalStateException();
        }

        if (bodyByte == -1) {
            isClosed = true;
        }

        try {
            bufferedBytes.put(bodyByte);
        } catch (InterruptedException e) {
            throw new ResponseStatusException(Status.REQUEST_TIMEOUT);
        }
    }

    public byte dequeue() {
        if (isClosed && bufferedBytes.isEmpty()) {
            return -1;
        }

        try {
            return bufferedBytes.take();
        } catch (InterruptedException e) {
            throw new ResponseStatusException(Status.REQUEST_TIMEOUT);
        }
    }
}
