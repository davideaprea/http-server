package shared.model;

import parser.dto.Header;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Request(
        Method method,
        Version version,
        RequestTarget target,
        Map<String, List<String>> headers,
        InputStream body
) {
    public static final class Builder {
        private Method method;
        private Version version;
        private RequestTarget requestTarget;
        private final Map<String, List<String>> headers = new HashMap<>();
        private InputStream body;

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
            headers.putIfAbsent(header.name(), new ArrayList<>());
            headers.get(header.name()).add(header.value());

            return this;
        }

        public Builder body(InputStream body) {
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
}
