package parser;

import model.Method;
import model.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RequestParser {
    public Request requestFrom(InputStream requestStream) throws IOException {
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(requestStream));
        String[] splitRequestLine = requestReader.readLine().split(" ");

        if (splitRequestLine.length != 3) {
            throw new IllegalStateException();
        }

        Method method = Method.valueOf(splitRequestLine[0]);
        String requestTarget = splitRequestLine[1];
        String version = splitRequestLine[2];

        if (!version.startsWith("HTTP/")) {
            throw new IllegalStateException();
        }

        Map<String, List<String>> queryParams = new HashMap<>();

        int paramsStartIndex = requestTarget.indexOf('?');

        if (paramsStartIndex > -1) {
            Arrays.stream(requestTarget
                            .substring(paramsStartIndex + 1)
                            .split("&"))
                    .map(rawParam -> rawParam.split("="))
                    .forEach(pair -> {
                        queryParams.putIfAbsent(pair[0], new ArrayList<>());
                        queryParams.get(pair[0]).add(pair[1]);
                    });

            requestTarget = requestTarget.substring(0, paramsStartIndex);
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
                queryParams,
                requestStream
        );
    }
}
