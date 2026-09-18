package reader.dto;

import lombok.Builder;
import model.Request;

import java.util.function.Consumer;

@Builder
public record ReadingLifecycleEvents(
        Consumer<Request> onNewRequest,
        Runnable onReadingAvailable,
        Runnable onStart,
        Runnable onEnd,
        Consumer<ReadingError> onError
) {
}
