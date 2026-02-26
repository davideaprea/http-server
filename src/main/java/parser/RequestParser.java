package parser;

import model.*;

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
        RequestLine requestLine = RequestLineParser.from(requestReader.readLine());
        Map<String, List<String>> headers = new HashMap<>();

        String currentLine;

        while (!(currentLine = requestReader.readLine()).isEmpty()) {
            Header header = HeaderParser.from(currentLine);

            headers.putIfAbsent(header.name(), new ArrayList<>());
            headers.get(header.name()).add(header.value());
        }

        return new Request(
                requestLine,
                headers,
                requestStream
        );
    }
}
