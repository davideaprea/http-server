package common.model;

import java.io.InputStream;
import java.util.Map;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        InputStream body
) {
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("%s %s %s\r\n".formatted(
                version.getValue(),
                status.getCode(),
                status.getName()
        ));

        for (Map.Entry<String, String> h : headers.entrySet()) {
            stringBuilder.append("%s: %s\r\n".formatted(h.getKey(), h.getValue()));
        }

        return stringBuilder.toString();
    }
}
