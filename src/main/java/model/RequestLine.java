package model;

public record RequestLine(
        Method method,
        String requestTarget,
        String version
) {
}
