package io.github.davideaprea.httpserver.model;

import io.github.davideaprea.httpserver.reader.exception.MalformedRequestException;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public Request(Method method, Version version, String url, Map<String, List<String>> queryParams, Map<String, List<String>> headers, RequestBody body) {
        this.method = method;
        this.version = version;
        this.url = url;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;

        var contentLength = Optional.ofNullable(headers.get(HeaderKey.CONTENT_LENGTH.getValue()));
        var transferEncoding = Optional.ofNullable(headers.get(HeaderKey.TRANSFER_ENCODING.getValue()));

        contentLength.ifPresent(values -> {
            if (values.size() != 1) {
                throw new MalformedRequestException("Content length header allows only one value.");
            }

            long value;

            try {
                value = Long.parseLong(values.getFirst());
            } catch (NumberFormatException e) {
                throw new MalformedRequestException("Content length is not a valid number.");
            }

            if (value < 0) {
                throw new MalformedRequestException("Content length header allows only zero or positive values.");
            }
        });

        transferEncoding.ifPresent(values -> {
            if (values.size() != 1 || !values.getFirst().equalsIgnoreCase("chunked")) {
                throw new MalformedRequestException("Unsupported transfer encoding");
            }
        });

        if (!headers.containsKey(HeaderKey.HOST.getValue())) {
            throw new MalformedRequestException("Mandatory header \"Host\" is missing.");
        }

        if (contentLength.isPresent() && transferEncoding.isPresent()) {
            throw new MalformedRequestException("One of content length or transfer encoding headers must be present in the request.");
        }
    }

    public Optional<String> getHeaderValue(String headerName) {
        return Optional.ofNullable(headers.get(headerName))
                .filter(values -> !values.isEmpty())
                .map(List::getFirst);
    }

    public Optional<Long> getContentLength() {
        return getHeaderValue(HeaderKey.CONTENT_LENGTH.getValue()).map(Long::parseLong);
    }

    /**
     * @return a <b>copy</b> of the query parameters map.
     */
    public Map<String, List<String>> getQueryParams() {
        return Map.copyOf(queryParams);
    }

    public boolean isClosingRequest() {
        return Optional.ofNullable(headers.get(HeaderKey.CONNECTION.getValue()))
                .filter(values -> !values.isEmpty())
                .map(values -> "close".equalsIgnoreCase(values.getFirst()))
                .orElse(false);
    }
}
