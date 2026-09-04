package client.writer;

import client.channel.ClientOutputChannel;
import model.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class TransferEncodingWriter extends ResponseBodyWriter {
    public TransferEncodingWriter(ClientOutputChannel clientOutputChannel) {
        super(clientOutputChannel);
    }

    @Override
    public void write(Response response) {
        byte[] bodyBytes = new byte[8192];

        try (InputStream bodyStream = response.body()) {
            int totalBytesRead;

            while ((totalBytesRead = bodyStream.read(bodyBytes)) != -1) {
                clientOutputChannel.write(String.valueOf(totalBytesRead).getBytes());
                clientOutputChannel.write("\r\n".getBytes());
                clientOutputChannel.write(bodyBytes);
                clientOutputChannel.write("\r\n".getBytes());
            }

            clientOutputChannel.write("0\r\n\r\n".getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
