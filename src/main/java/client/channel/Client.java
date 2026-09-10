package client.channel;

public record Client(
        ClientChannelKey channelKey,
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
