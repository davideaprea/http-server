package parser.dto;

import model.Method;
import model.Version;

public record RequestLine(
        Method method,
        String requestTarget,
        Version version
) {
}
