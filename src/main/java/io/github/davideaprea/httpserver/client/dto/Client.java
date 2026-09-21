package io.github.davideaprea.httpserver.client.dto;

import io.github.davideaprea.httpserver.client.ClientInputChannel;
import io.github.davideaprea.httpserver.client.ClientOutputChannel;

public record Client(
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
