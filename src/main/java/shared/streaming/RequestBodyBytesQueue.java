package shared.streaming;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class RequestBodyBytesQueue implements BodyBytesDequeue, BodyBytesEnqueue {
    private final Queue<Byte> bufferedBytes = new LinkedList<>();

    public void enqueue(byte bodyByte) {
        bufferedBytes.add(bodyByte);
    }

    public Optional<Byte> dequeue() {
        return Optional.ofNullable(bufferedBytes.poll());
    }
}
