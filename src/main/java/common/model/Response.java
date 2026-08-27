package common.model;

import java.util.Map;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        ResponseBody body
) {
    public String responseLine() {
        return "%s %s %s".formatted(
                version.getValue(),
                status.getCode(),
                status.getName()
        );
    }
}
