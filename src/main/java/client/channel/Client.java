package client.channel;

public record Client(
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
