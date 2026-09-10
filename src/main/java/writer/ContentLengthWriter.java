package writer;

import client.ClientOutputChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ContentLengthWriter extends ResponseBodyWriter {
    public ContentLengthWriter(ClientOutputChannel clientOutputChannel) {
        super(clientOutputChannel);
    }

    @Override
    public void fromSource(InputStream bodyStream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead;

        while ((bytesRead = bodyStream.read(buffer.array())) != -1) {
            buffer.position(0);
            buffer.limit(bytesRead);

            clientOutputChannel.write(buffer.array(), false);

            buffer.clear();
        }
    }
}
