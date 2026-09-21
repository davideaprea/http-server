package io.github.davideaprea.httpserver.client.dto;

import java.nio.ByteBuffer;

public record OutputChunk(
        ByteBuffer value,
        boolean isLast
) {
}
