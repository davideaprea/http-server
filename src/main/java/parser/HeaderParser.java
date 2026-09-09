package parser;

import common.MalformedRequestException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import parser.dto.Header;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderParser {
    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (headerLine.length() <= 1 || separatorIndex == -1) {
            throw new MalformedRequestException("Invalid header format.");
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.isEmpty() || headerName.chars().anyMatch(Character::isWhitespace)) {
            throw new MalformedRequestException("Header name contains invalid space characters.");
        }

        return new Header(headerName.toLowerCase(), headerValue);
    }
}
