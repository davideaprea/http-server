package parsing.model;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public record Request(
        Method method,
        Version version,
        RequestTarget target,
        Map<String, List<String>> headers,
        InputStream body
) {
}
