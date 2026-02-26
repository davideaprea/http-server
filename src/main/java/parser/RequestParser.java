package parser;

import model.Header;
import model.Method;
import model.Request;
import model.RequestTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RequestParser {
    private RequestParser() {
    }

    public static Request from(InputStream requestStream) throws IOException {
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(requestStream));
        String[] splitRequestLine = requestReader.readLine().split(" ");

        if (splitRequestLine.length != 3) {
            throw new IllegalStateException();
        }

        Method method = Method.valueOf(splitRequestLine[0]);
        RequestTarget requestTarget = RequestTargetParser.from(splitRequestLine[1]);
        String version = splitRequestLine[2];

        if (!version.startsWith("HTTP/")) {
            throw new IllegalStateException();
        }

        Map<String, List<String>> headers = new HashMap<>();

        String currentLine;

        while (!(currentLine = requestReader.readLine()).isEmpty()) {
            Header header = HeaderParser.from(currentLine);

            headers.putIfAbsent(header.name(), new ArrayList<>());
            headers.get(header.name()).add(header.value());
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
