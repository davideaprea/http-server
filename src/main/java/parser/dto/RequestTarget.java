package parser.dto;

import common.MultiValueMap;

public record RequestTarget(
        String url,
        MultiValueMap<String, String> queryParams
) {
}
