package io.github.davideaprea.httpserver.connection.dto;

import io.github.davideaprea.httpserver.connection.channel.ClientChannelKey;
import io.github.davideaprea.httpserver.connection.channel.ClientResponsesQueue;
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
