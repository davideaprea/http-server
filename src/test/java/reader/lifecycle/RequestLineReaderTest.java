package reader.lifecycle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = mockRequestLineReader();
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        RequestLineReader reader = mockRequestLineReader();
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = mockRequestLineReader();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        state = state.eval((byte) '\r').nextReader();
        state = state.eval((byte) '\n').nextReader();

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    private RequestLineReader mockRequestLineReader() {
        return new RequestLineReader(new ReadingLifecycleEvents(
                request -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        ), new SizeLimits(1000, 1000));
    }
}
