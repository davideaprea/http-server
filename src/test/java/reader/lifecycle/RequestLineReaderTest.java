package reader.lifecycle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reader.dto.ReadingLifecycleEvents;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(mockReadingLifecycleEvents());
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        RequestLineReader reader = new RequestLineReader(mockReadingLifecycleEvents());
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = new RequestLineReader(mockReadingLifecycleEvents());

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        state = state.eval((byte) '\r').nextReader();
        state = state.eval((byte) '\n').nextReader();

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    private ReadingLifecycleEvents mockReadingLifecycleEvents() {
        return new ReadingLifecycleEvents(
                request -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );
    }
}
