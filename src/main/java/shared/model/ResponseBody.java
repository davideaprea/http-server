package shared.model;

import java.util.function.Consumer;

public class ResponseBody {
    private Consumer<byte[]> consumer;

    public void subscribe(Consumer<byte[]> consumer) {
        this.consumer = consumer;
    }

    public void emit(byte[] chunk) {
        if (consumer != null) {
            consumer.accept(chunk);
        }
    }
}
