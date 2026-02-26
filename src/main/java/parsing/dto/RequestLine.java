package parsing.dto;

import parsing.model.Method;
import parsing.model.Version;

public record RequestLine(
        Method method,
        String requestTarget,
        Version version
) {
}
