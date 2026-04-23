package parser;

import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.Method;
import shared.model.Status;
import shared.model.Version;

public class RequestLineParser {
    public RequestLine from(String rawRequestLine) {
        String[] splitRequestLine = rawRequestLine.split(" ");

        if (splitRequestLine.length != 3) {
            throw new ResponseStatusException("Request line is malformed.", Status.BAD_REQUEST);
        }

        Method method;

        try {
            method = Method.valueOf(splitRequestLine[0]);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException("Invalid method.", Status.BAD_REQUEST);
        }

        String requestTarget = splitRequestLine[1];
        Version version = Version.fromValue(splitRequestLine[2]);

        return new RequestLine(method, requestTarget, version);
    }
}
