package reader.dto;

import common.queue.BodyBytesEnqueue;

public record ContentLengthRequest(
        BodyBytesEnqueue body,
        long bytesNumber
) {
}
