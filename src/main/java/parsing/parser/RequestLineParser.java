package parsing.parser;

import parsing.model.Method;
import parsing.dto.RequestLine;
import parsing.model.Version;

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
        Version version = Version.fromValue(splitRequestLine[2]);

        return new RequestLine(method, requestTarget, version);
    }
}
