package writer;

import client.ClientOutputChannel;

import java.io.IOException;
import java.io.InputStream;

public abstract class ResponseBodyWriter {
    protected final ClientOutputChannel clientOutputChannel;

    public ResponseBodyWriter(ClientOutputChannel clientOutputChannel) {
        this.clientOutputChannel = clientOutputChannel;
    }

    public abstract void fromSource(InputStream bodyStream) throws IOException;
}
