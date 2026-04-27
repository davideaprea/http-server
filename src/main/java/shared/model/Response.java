package shared.model;

import java.io.InputStream;
import java.util.Map;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        InputStream body
) {
}
