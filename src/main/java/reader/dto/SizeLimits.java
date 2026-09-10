package reader.dto;

public record SizeLimits(
        long maxHeadersSize,
        long maxBodySize
) {
}
