package parser;

import parser.dto.Header;
import exception.MalformedHeaderException;

public class HeaderParser {
    private HeaderParser() {
    }

    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (separatorIndex == -1) {
            throw new MalformedHeaderException(headerLine, "Name and value must be separated by a colon (:) character.");
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.contains(" ")) {
            throw new MalformedHeaderException(headerLine, "Header name contains invalid space characters.");
        }

        return new Header(headerName, headerValue);
    }
}
