package model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

        headers = new HashMap<>(headers);
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

    public static Response badRequestError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : Status.BAD_REQUEST.getName();
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        return new Response(
                Version.HTTP_1_1,
                Status.BAD_REQUEST,
                Map.of(
                        HeaderKey.CONNECTION.getValue(), "close",
                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                        HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(body.length)
                ),
                new ByteArrayInputStream(body)
        );
    }
}
