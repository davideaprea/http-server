package client.channel;

import java.nio.ByteBuffer;

public record OutputChunk(
        ByteBuffer value,
        boolean isLast
) {
}
