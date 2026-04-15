package request.parser;

import request.dto.Header;
import request.model.Request;
import request.dto.RequestLine;
import request.model.RequestTarget;

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
        RequestTarget requestTarget = RequestTargetParser.from(requestLine.requestTarget());
        Map<String, List<String>> headers = new HashMap<>();

        String currentLine;

        while (!(currentLine = requestReader.readLine()).isEmpty()) {
            Header header = HeaderParser.from(currentLine);

            headers.putIfAbsent(header.name(), new ArrayList<>());
            headers.get(header.name()).add(header.value());
        }

        return new Request(
                requestLine.method(),
                requestLine.version(),
                requestTarget,
                headers,
                requestStream
        );
    }
}
