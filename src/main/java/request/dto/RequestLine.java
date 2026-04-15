package request.dto;

import request.model.Method;
import request.model.Version;

public record RequestLine(
        Method method,
        String requestTarget,
        Version version
) {
}
