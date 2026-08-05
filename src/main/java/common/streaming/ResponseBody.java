package common.streaming;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@AllArgsConstructor
public class ResponseBody {
    private final InputStream sourceStream;

    public void subscribe(
            Consumer<byte[]> onBodyChunk,
            Runnable onBodyEnd
    ) {
        try {
            byte[] bodyChunk;

            while (sourceStream.read((bodyChunk = new byte[4096])) >= 0) {
                onBodyChunk.accept(bodyChunk);
            }

            onBodyEnd.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
