package reader.dto;

import shared.streaming.BodyBytesEnqueue;

public record ContentLengthRequest(
        BodyBytesEnqueue body,
        long bytesNumber
) {
}
