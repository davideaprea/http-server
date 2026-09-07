package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Builder
public class Request {
    @Getter
    private final Method method;
    private final Version version;

    @Getter
    private final String url;

    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> headers;

    @Getter
    private final RequestBody body;

    public Map<String, List<String>> getHeaders() {
        return Map.copyOf(headers);
    }

    public Map<String, List<String>> getQueryParams() {
        return Map.copyOf(queryParams);
    }
}
