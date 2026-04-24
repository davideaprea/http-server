package parser;

import parser.dto.Header;
import parser.dto.RequestLine;
import shared.model.Request;
import shared.model.RequestTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.*;

public class RequestParser {
    private final HeaderParser headerParser = new HeaderParser();
    private final RequestLineParser requestLineParser = new RequestLineParser();
    private final RequestTargetParser requestTargetParser = new RequestTargetParser();

    public Request from(Socket clientSocket) throws IOException {
        InputStream requestStream = clientSocket.getInputStream();
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

        return new Request(
                requestLine.method(),
                requestLine.version(),
                requestTarget,
                headers,
                requestStream
        );
    }
}
