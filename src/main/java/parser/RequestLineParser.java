package parser;

import model.Method;
import model.RequestLine;
import model.RequestTarget;

public class RequestLineParser {
    private RequestLineParser() {
    }

    public static RequestLine from(String rawRequestLine) {
        String[] splitRequestLine = rawRequestLine.split(" ");

        if (splitRequestLine.length != 3) {
            throw new IllegalStateException();
        }

        Method method = Method.valueOf(splitRequestLine[0]);
        RequestTarget requestTarget = RequestTargetParser.from(splitRequestLine[1]);
        String version = splitRequestLine[2];

        if (!version.startsWith("HTTP/")) {
            throw new IllegalStateException();
        }

        return new RequestLine(method, requestTarget, version);
    }
}
