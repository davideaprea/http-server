package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final RequestBody body;

    public Optional<Long> getContentLength() {
        return Optional
                .ofNullable(headers.get(HeaderKey.CONTENT_LENGTH.getValue()))
                .map(List::getFirst)
                .map(Long::parseLong);
    }

    public Optional<String> getTransferEncoding() {
        return Optional
                .ofNullable(headers.get(HeaderKey.TRANSFER_ENCODING.getValue()))
                .map(List::getFirst);
    }
}
