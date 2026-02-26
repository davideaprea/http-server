package parsing.parser;

import parsing.model.Method;
import parsing.dto.RequestLine;

public class RequestLineParser {
    private RequestLineParser() {
    }

    public static RequestLine from(String rawRequestLine) {
        String[] splitRequestLine = rawRequestLine.split(" ");

        if (splitRequestLine.length != 3) {
            throw new IllegalStateException();
        }

        Method method = Method.valueOf(splitRequestLine[0]);
        String requestTarget = splitRequestLine[1];
        String version = splitRequestLine[2];

        if (!version.startsWith("HTTP/")) {
            throw new IllegalStateException();
        }

        return new RequestLine(method, requestTarget, version);
    }
}
