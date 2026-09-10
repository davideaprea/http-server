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
            clientOutputChannel.write((Integer.toHexString(bytesRead) + "\r\n").getBytes(), false);
            buffer.position(0);
            buffer.limit(bytesRead);
            clientOutputChannel.write(buffer.array(), false);
            clientOutputChannel.write("\r\n".getBytes(), false);
            buffer.clear();
        }

        clientOutputChannel.write("0\r\n\r\n".getBytes(), false);
    }
}
