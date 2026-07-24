package shared.streaming;

import java.util.Optional;

public interface BodyBytesDequeue {
    Optional<Byte> dequeue();
}
