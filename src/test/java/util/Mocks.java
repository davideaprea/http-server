package util;

import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.connection.channel.ClientChannel;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.router.Router;
import org.mockito.Mockito;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class Mocks {
    public static ClientChannel clientChannel() {
        SelectionKey selectionKey = Mockito.mock(SelectionKey.class);
        SocketChannel socketChannel = Mockito.mock(SocketChannel.class);

        Mockito.when(selectionKey.channel()).thenReturn(socketChannel);
        Mockito.when(socketChannel.isOpen()).thenReturn(true);

        return new ClientChannel(
                selectionKey,
                Mockito.mock(TimedOperation.class),
                new SizeLimits(
                        Long.MAX_VALUE,
                        Long.MAX_VALUE
                ),
                Mockito.mock(Router.class),
                Mockito.mock(ExecutorService.class)
        );
    }
}
