package parser;

import shared.exception.ResponseStatusException;
import shared.model.Status;
import parser.dto.Header;

public final class HeaderParser {
    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (separatorIndex == -1) {
            throw new ResponseStatusException("Name and value must be separated by a colon (:) character.", Status.BAD_REQUEST);
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.contains(" ")) {
            throw new ResponseStatusException("Header name contains invalid space characters.", Status.BAD_REQUEST);
        }

        return new Header(headerName, headerValue);
    }
}
