package parser.dto;

import model.Method;
import model.Version;

import java.util.List;
import java.util.Map;

public record RequestTarget(
        Method method,
        Version version,
        String url,
        Map<String, List<String>> queryParams
) {
}
