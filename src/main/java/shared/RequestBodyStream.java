package shared;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestBodyStream extends InputStream {
    BlockingQueue<Byte> byteQueue = new LinkedBlockingQueue<>();

    @Override
    public int read() throws IOException {
        var b = byteQueue.poll();

        if (b == null) {
            return -1;
        }

        return b & 0xFF;
    }

    public void append(byte b) {
        byteQueue.add(b);
    }
}
