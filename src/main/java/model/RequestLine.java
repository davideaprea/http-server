package model;

public record RequestLine(
        Method method,
        RequestTarget requestTarget,
        String version
) {
}
