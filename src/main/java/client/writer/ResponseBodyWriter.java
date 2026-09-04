package client.writer;

import client.channel.ClientOutputChannel;
import model.Response;

public abstract class ResponseBodyWriter {
    protected final ClientOutputChannel clientOutputChannel;

    protected ResponseBodyWriter(ClientOutputChannel clientOutputChannel) {
        this.clientOutputChannel = clientOutputChannel;
    }

    public abstract void write(Response response);
}
