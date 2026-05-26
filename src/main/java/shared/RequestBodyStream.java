package shared;

import lombok.Getter;

import java.io.InputStream;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestBodyStream extends InputStream {
    private final Queue<Byte> buffer = new LinkedBlockingQueue<>();

    @Getter
    private boolean closed = false;

    public void append(byte data) {
        if (closed) {
            return;
        }

        buffer.add(data);
    }

    @Override
    public int read() {
        return Optional.ofNullable(buffer.poll()).orElse((byte) -1) & 0xFF;
    }

    @Override
    public void close() {
        closed = true;
    }
}
