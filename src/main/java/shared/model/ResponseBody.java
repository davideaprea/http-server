package shared.model;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@AllArgsConstructor
public class ResponseBody {
    private final InputStream sourceStream;

    public void subscribe(Consumer<byte[]> consumer) {
        try {
            byte[] bodyChunk;

            while (sourceStream.read((bodyChunk = new byte[4096])) >= 0) {
                consumer.accept(bodyChunk);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
