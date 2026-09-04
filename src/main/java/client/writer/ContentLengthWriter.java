package client.writer;

import client.channel.ClientOutputChannel;
import model.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ContentLengthWriter extends ResponseBodyWriter {
    public ContentLengthWriter(ClientOutputChannel clientOutputChannel) {
        super(clientOutputChannel);
    }

    @Override
    public void write(Response response) {
        byte[] bodyBytes = new byte[8192];

        try (InputStream bodyStream = response.body()) {
            while (bodyStream.read(bodyBytes) != -1) {
                clientOutputChannel.write(bodyBytes);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
