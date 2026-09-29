package io.github.davideaprea.httpserver.reader.dto;

import io.github.davideaprea.httpserver.client.ClientChannelKey;
import io.github.davideaprea.httpserver.client.ClientResponsesQueue;
import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.router.Router;

public record Context(
        ClientChannelKey channelKey,
        TimedOperation requestTimer,
        ClientResponsesQueue responsesQueue,
        SizeLimits sizeLimits,
        Router router
) {
}
