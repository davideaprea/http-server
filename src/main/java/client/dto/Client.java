package client.dto;

import client.ClientInputChannel;
import client.ClientOutputChannel;

public record Client(
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
