package parser;

import model.Method;
import model.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParser {
    public Request requestFrom(InputStream requestStream) throws IOException {
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(requestStream));
        final String[] splitRequestLine = requestReader.readLine().split(" ");

        if (splitRequestLine.length != 3) {
            throw new IllegalStateException();
        }

        final Method method = Method.valueOf(splitRequestLine[0]);
        final String requestTarget = splitRequestLine[1];
        final String version = splitRequestLine[2];

        if (!version.startsWith("HTTP/")) {
            throw new IllegalStateException();
        }

        Map<String, List<String>> headers = new HashMap<>();

        String currentLine;

        while (!(currentLine = requestReader.readLine()).isEmpty()) {
            final int separatorIndex = currentLine.indexOf(':');

            if (separatorIndex == -1) {
                throw new IllegalStateException();
            }

            final String headerName = currentLine.substring(0, separatorIndex);
            final String headerValue = currentLine.substring(separatorIndex + 1).trim();

            if (headerName.contains(" ")) {
                throw new IllegalStateException();
            }

            headers.putIfAbsent(headerName, new ArrayList<>());
            headers.get(headerName).add(headerValue);
        }

        return new Request(
                method,
                requestTarget,
                version,
                headers,
                requestStream
        );
    }
}
