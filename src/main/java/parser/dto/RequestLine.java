package parser.dto;

import shared.model.Method;
import shared.model.Version;

public record RequestLine(
        Method method,
        String requestTarget,
        Version version
) {
}
