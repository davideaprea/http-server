package parser.dto;

import common.MultiValueMap;
import model.Method;
import model.Version;

public record RequestTarget(
        Method method,
        Version version,
        String url,
        MultiValueMap<String, String> queryParams
) {
}
