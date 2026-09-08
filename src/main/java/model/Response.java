package model;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        InputStream body
) {
    public Response {
        Objects.requireNonNull(version);
        Objects.requireNonNull(status);
        Objects.requireNonNull(body);
    }

    public String toHTTPFrame() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("%s %s %s\r\n".formatted(
                version.getValue(),
                status.getCode(),
                status.getName()
        ));

        for (Map.Entry<String, String> h : headers.entrySet()) {
            stringBuilder.append("%s: %s\r\n".formatted(h.getKey(), h.getValue()));
        }

        stringBuilder.append("\r\n");

        return stringBuilder.toString();
    }
}
