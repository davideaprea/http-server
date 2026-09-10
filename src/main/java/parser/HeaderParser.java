package parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import parser.dto.Header;
import parser.exception.BadFormatException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderParser {
    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (headerLine.length() <= 1 || separatorIndex == -1) {
            throw new BadFormatException("Invalid header format.");
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.isEmpty() || headerName.chars().anyMatch(Character::isWhitespace)) {
            throw new BadFormatException("Header name <%s> contains invalid space characters.".formatted(headerName));
        }

        return new Header(headerName.toLowerCase(), headerValue);
    }
}
