package model;

import java.util.Map;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        Object body
) {
}
