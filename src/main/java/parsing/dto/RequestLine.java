package parsing.dto;

import parsing.model.Method;

public record RequestLine(
        Method method,
        String requestTarget,
        String version
) {
}
