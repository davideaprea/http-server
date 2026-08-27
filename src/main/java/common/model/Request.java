package common.model;

import parser.dto.Header;
import common.queue.BodyBytesDequeue;

import java.util.*;

public record Request(
        Method method,
        Version version,
        RequestTarget target,
        Map<String, List<String>> headers,
        BodyBytesDequeue body
) {
    public static final class Builder {
        private Method method;
        private Version version;
        private RequestTarget requestTarget;
        private final Map<String, List<String>> headers = new HashMap<>();
        private BodyBytesDequeue body;

        public Builder method(Method method) {
            this.method = method;

            return this;
        }

        public Builder version(Version version) {
            this.version = version;

            return this;
        }

        public Builder requestTarget(RequestTarget requestTarget) {
            this.requestTarget = requestTarget;

            return this;
        }

        public Builder header(Header header) {
            headers.putIfAbsent(header.name().toLowerCase(), new ArrayList<>());
            headers.get(header.name()).add(header.value());

            return this;
        }

        public Builder body(BodyBytesDequeue body) {
            this.body = body;

            return this;
        }

        public Request build() {
            return new Request(
                    method,
                    version,
                    requestTarget,
                    headers,
                    body
            );
        }
    }

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
