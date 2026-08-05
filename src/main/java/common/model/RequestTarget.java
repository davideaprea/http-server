package common.model;

import java.util.List;
import java.util.Map;

public record RequestTarget(
        String url,
        Map<String, List<String>> queryParams
) {
}
