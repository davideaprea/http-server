package io.github.davideaprea.httpserver.reader.dto;

public record SizeLimits(
        long maxHeadersSize,
        long maxBodySize
) {
    public SizeLimits {
        if (maxBodySize < 0 || maxHeadersSize < 0) {
            throw new IllegalArgumentException("Size limits must be >= 0.");
        }
    }
}
