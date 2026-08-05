package common.model;

import common.streaming.ResponseBody;

import java.util.Map;

public record Response(
        Version version,
        Status status,
        Map<String, String> headers,
        ResponseBody body
) {
}
