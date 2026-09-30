package io.github.davideaprea.httpserver.connection.dto;

import io.github.davideaprea.httpserver.connection.channel.ClientInputChannel;
import io.github.davideaprea.httpserver.connection.channel.ClientOutputChannel;

public record Channel(
        ClientInputChannel input,
        ClientOutputChannel output
) {
}
