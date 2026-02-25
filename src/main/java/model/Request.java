package model;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public record Request(
        Method method,
        String targetURL,
        String version,
        Map<String, List<String>> headers,
        InputStream body
) {
}
