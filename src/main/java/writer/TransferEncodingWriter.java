package writer;

import client.ClientOutputChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TransferEncodingWriter extends ResponseBodyWriter {
    public TransferEncodingWriter(ClientOutputChannel clientOutputChannel) {
        super(clientOutputChannel);
    }

    @Override
    public void fromSource(InputStream bodyStream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead;

        while ((bytesRead = bodyStream.read(buffer.array())) != -1) {
            clientOutputChannel.write(ByteBuffer.wrap((Integer.toHexString(bytesRead) + "\r\n").getBytes()));
            buffer.position(0);
            buffer.limit(bytesRead);
            clientOutputChannel.write(buffer);
            clientOutputChannel.write(ByteBuffer.wrap("\r\n".getBytes()));
            buffer.clear();
        }

        clientOutputChannel.write(ByteBuffer.wrap("0\r\n\r\n".getBytes()));
    }
}
