package parser;

import parser.dto.Header;
import model.Request;
import parser.dto.RequestLine;
import model.RequestTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RequestParser {
    public Request from(InputStream requestStream) throws IOException {
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
