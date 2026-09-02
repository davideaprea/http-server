package parser;

import common.exception.ResponseStatusException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import model.Status;
import parser.dto.Header;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderParser {
    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (separatorIndex == -1) {
            throw new ResponseStatusException("Name and value must be separated by a colon (:) character.", Status.BAD_REQUEST);
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.isEmpty() || headerName.chars().anyMatch(Character::isWhitespace)) {
            throw new ResponseStatusException("Header name contains invalid space characters.", Status.BAD_REQUEST);
        }

        return new Header(headerName.toLowerCase(), headerValue);
    }
}
