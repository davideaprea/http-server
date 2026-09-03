package parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import parser.dto.Header;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderParser {
    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Header name and value must be separated by a colon (:) character.");
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.isEmpty() || headerName.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Header name contains invalid space characters.");
        }

        return new Header(headerName.toLowerCase(), headerValue);
    }
}
