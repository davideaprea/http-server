package reader.dto;

import common.streaming.BodyBytesEnqueue;

public record ContentLengthRequest(
        BodyBytesEnqueue body,
        long bytesNumber
) {
}
