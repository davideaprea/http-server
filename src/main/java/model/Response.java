package model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

        headers.put(HeaderKey.DATE.getValue(), ZonedDateTime
                .now(ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME));
    }

    /**
     * Builds the response header section in the HTTP format, including the status line and
     * response headers.
     *
     * @return the HTTP response header section as a string
     */
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
        Response response = textResponse(message, Status.BAD_REQUEST);

        response.headers.put(HeaderKey.CONNECTION.getValue(), "close");

        return response;
    }

    public static Response textResponse(String body, Status status) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        return new Response(
                Version.HTTP_1_1,
                status,
                Map.of(
                        HeaderKey.CONTENT_TYPE.getValue(), "text/plain",
                        HeaderKey.CONTENT_LENGTH.getValue(), String.valueOf(bodyBytes.length)
                ),
                new ByteArrayInputStream(bodyBytes)
        );
    }
}
