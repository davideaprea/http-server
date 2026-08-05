package reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import common.streaming.RequestQueue;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(RequestQueue.class));
        ReadingState result = reader.eval((byte) 'G');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingLineFeed() {
        RequestQueue context = Mockito.mock(RequestQueue.class);
        RequestLineReader reader = new RequestLineReader(context);
        ReadingState result = reader.eval((byte) '\n');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        RequestQueue context = Mockito.mock(RequestQueue.class);
        String rawRequest = "GET /path HTTP/1.1";
        ReadingState state = new RequestLineReader(context);

        for(int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        state = state.eval((byte) '\n');
        state = state.eval((byte) '\r');

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }
}
