package parser;

import model.Header;

public class HeaderParser {
    private HeaderParser() {
    }

    public static Header from(String headerLine) {
        final int separatorIndex = headerLine.indexOf(':');

        if (separatorIndex == -1) {
            throw new IllegalStateException();
        }

        final String headerName = headerLine.substring(0, separatorIndex);
        final String headerValue = headerLine.substring(separatorIndex + 1).trim();

        if (headerName.contains(" ")) {
            throw new IllegalStateException();
        }

        return new Header(headerName, headerValue);
    }
}
