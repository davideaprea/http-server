package client;

import reader.ReadResult;
import reader.ReadingLifecycleEvents;
import reader.RequestLineReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientInputChannel {
    private final ClientChannelKey clientChannelKey;
    private final ByteBuffer buffer;

    private ReadResult readResult;

    public ClientInputChannel(ClientChannelKey clientChannelKey, ReadingLifecycleEvents readingLifecycleEvents) {
        this.clientChannelKey = clientChannelKey;
        buffer = ByteBuffer.allocateDirect(8192);
        readResult = new ReadResult(
                new RequestLineReader(readingLifecycleEvents),
                ReadResult.NextAction.PROCEED
        );
    }

    public void read() {
        if (readResult.nextAction().equals(ReadResult.NextAction.WAIT)) {
            return;
        }

        SocketChannel client = (SocketChannel) clientChannelKey.getSocketChannel();
        int bytesRead;

        while (true) {
            try {
                bytesRead = client.read(buffer);

                if (bytesRead <= 0) break;

                buffer.flip();

                while (buffer.hasRemaining() && readResult.nextAction().equals(ReadResult.NextAction.PROCEED)) {
                    readResult = readResult.nextReader().eval(buffer.get());
                }

                buffer.clear();
            } catch (IOException e) {
                System.out.println("Error while reading from client socket: " + e.getMessage());

                clientChannelKey.close();

                return;
            }
        }

        if (readResult.nextAction().equals(ReadResult.NextAction.WAIT)) {
            clientChannelKey.removeReadInterest();
        }

        if (bytesRead == -1) {
            clientChannelKey.close();
        }
    }
}
