package parser;

import parser.dto.Header;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class RequestParser {
    private final HeaderParser headerParser = new HeaderParser();
    private final RequestLineParser requestLineParser = new RequestLineParser();
    private final RequestTargetParser requestTargetParser = new RequestTargetParser();

    public Request from(InputStream requestStream) throws IOException {
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(requestStream));
        RequestLine requestLine = requestLineParser.from(requestReader.readLine());
        RequestTarget requestTarget = requestTargetParser.from(requestLine.requestTarget());
        Map<String, List<String>> headers = new HashMap<>();

        String currentLine;

        while (!(currentLine = Optional
                .ofNullable(requestReader.readLine())
                .orElse(""))
                .isEmpty()) {
            Header header = headerParser.from(currentLine);

            headers.putIfAbsent(header.name(), new ArrayList<>());
            headers.get(header.name()).add(header.value());
        }

        if (
                !requestLine.method().equals(Method.GET) &&
                !requestLine.method().equals(Method.HEAD) &&
                !headers.containsKey(HeaderKey.CONTENT_LENGTH.getValue())
        ) {
            throw new ResponseStatusException("Headers 'Content-Length' or 'Transfer-Encoding' are mandatory.", Status.BAD_REQUEST);
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
