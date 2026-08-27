package common.queue;

import common.exception.ResponseStatusException;
import common.model.Status;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestBodyBytesQueue implements BodyBytesDequeue, BodyBytesEnqueue {
    private final BlockingQueue<Integer> bufferedBytes = new LinkedBlockingQueue<>();

    public void enqueue(int bodyByte) {
        try {
            bufferedBytes.put(bodyByte);
        } catch (InterruptedException e) {
            throw new ResponseStatusException(Status.REQUEST_TIMEOUT);
        }
    }

    public int dequeue() {
        try {
            return bufferedBytes.take();
        } catch (InterruptedException e) {
            throw new ResponseStatusException(Status.REQUEST_TIMEOUT);
        }
    }
}
