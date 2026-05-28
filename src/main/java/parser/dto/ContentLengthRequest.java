package parser.dto;

import shared.RequestBodyStream;

public record ContentLengthRequest(
        RequestBodyStream body,
        long bytesNumber
) {
}
