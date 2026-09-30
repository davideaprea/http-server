package io.github.davideaprea.httpserver.connection.dto;

import java.nio.ByteBuffer;

public record OutputChunk(
        ByteBuffer value,
        boolean isLast
) {
}
