package client;

public record Client(
        ClientInputChannel inputChannel,
        ClientOutputChannel outputChannel
) {
}
