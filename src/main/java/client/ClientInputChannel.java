package client;

import common.TimedOperation;
import reader.ReadResult;
import reader.ReadingLifecycleEvents;
import reader.RequestLineReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientInputChannel extends ClientChannel {
    private final ByteBuffer buffer;

    private ReadResult readResult;

    public ClientInputChannel(TimedOperation timedOperation, SelectionKey clientKey, ClientRequestsQueue clientRequestsQueue) {
        super(clientKey);

        buffer = ByteBuffer.allocateDirect(8192);
        readResult = new ReadResult(
                new RequestLineReader(
                        ReadingLifecycleEvents.builder()
                                .onNewRequest(clientRequestsQueue::enqueue)
                                .onReadingAvailable(() -> {
                                    clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_READ);
                                    clientKey.selector().wakeup();
                                })
                                .onStart(timedOperation::start)
                                .onEnd(timedOperation::stop)
                                .build()
                ),
                ReadResult.NextAction.PROCEED
        );
    }

    public void read() {
        if (readResult.nextAction().equals(ReadResult.NextAction.WAIT)) {
            return;
        }

        SocketChannel client = (SocketChannel) clientKey.channel();
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

                close();

                return;
            }
        }

        if (readResult.nextAction().equals(ReadResult.NextAction.WAIT)) {
            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
        }

        if (bytesRead == -1) {
            close();
        }
    }
}
