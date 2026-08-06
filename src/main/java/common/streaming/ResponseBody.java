package common.streaming;

import java.util.function.Consumer;

public interface ResponseBody {
    void subscribe(Consumer<byte[]> onBodyChunk, Runnable onBodyEnd);
}
