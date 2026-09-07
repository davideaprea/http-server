package reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.dto.ReadingLifecycleEvents;
import reader.lifecycle.HeadersReader;
import reader.lifecycle.RequestLineReader;
import reader.lifecycle.RequestReader;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(ReadingLifecycleEvents.class));
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(ReadingLifecycleEvents.class));
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = new RequestLineReader(Mockito.mock(ReadingLifecycleEvents.class));

        for(int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        state = state.eval((byte) '\r').nextReader();
        state = state.eval((byte) '\n').nextReader();

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }
}
