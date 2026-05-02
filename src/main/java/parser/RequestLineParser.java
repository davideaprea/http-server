package parser;

import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.Method;
import shared.model.Status;
import shared.model.Version;

import java.util.NoSuchElementException;

public final class RequestLineParser {
    public static RequestLine from(String rawRequestLine) {
        String[] splitRequestLine = rawRequestLine.split(" ");

        if (splitRequestLine.length != 3) {
            throw new ResponseStatusException("Request line is malformed.", Status.BAD_REQUEST);
        }

        return new RequestLine(
                parseMethod(splitRequestLine[0]),
                splitRequestLine[1],
                parseVersion(splitRequestLine[2])
        );
    }

    private static Method parseMethod(String method) {
        try {
            return Method.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException("Invalid method.", Status.BAD_REQUEST);
        }
    }

    private static Version parseVersion(String version) {
        try {
            return Version.fromValue(version);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException("Unsupported HTTP version: " + version, Status.VERSION_NOT_SUPPORTED);
        }
    }
}
