package client;

public record Client(
        ClientChannelKey channelKey,
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
