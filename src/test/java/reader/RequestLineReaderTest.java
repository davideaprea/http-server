package reader;

import common.queue.RequestQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(RequestQueue.class), () -> {});
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingLineFeed() {
        RequestQueue context = Mockito.mock(RequestQueue.class);
        RequestLineReader reader = new RequestLineReader(context, () -> {});
        RequestReader result = reader.eval((byte) '\n').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        RequestQueue context = Mockito.mock(RequestQueue.class);
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = new RequestLineReader(context, () -> {});

        for(int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        state = state.eval((byte) '\n').nextReader();
        state = state.eval((byte) '\r').nextReader();

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }
}
