package util;

import io.github.davideaprea.httpserver.connection.channel.ClientChannelKey;
import io.github.davideaprea.httpserver.connection.channel.ClientResponsesQueue;
import io.github.davideaprea.httpserver.common.TimedOperation;
import io.github.davideaprea.httpserver.connection.dto.Context;
import io.github.davideaprea.httpserver.connection.dto.SizeLimits;
import io.github.davideaprea.httpserver.router.Router;
import org.mockito.Mockito;

public class Mocks {
    public static Context context() {
        return new Context(
                Mockito.mock(ClientChannelKey.class),
                Mockito.mock(TimedOperation.class),
                Mockito.mock(ClientResponsesQueue.class),
                new SizeLimits(
                        Long.MAX_VALUE,
                        Long.MAX_VALUE
                ),
                Mockito.mock(Router.class)
        );
    }
}
