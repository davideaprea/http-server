package parser.dto;

import common.model.Method;
import common.model.Version;

public record RequestLine(
        Method method,
        String requestTarget,
        Version version
) {
}
