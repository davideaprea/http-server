package reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import common.model.Request;
import common.streaming.RequestQueue;

public class HeadersReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        HeadersReader reader = withMocks();
        ReadingState result = reader.eval((byte) 'G');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingLineFeed() {
        HeadersReader reader = withMocks();
        ReadingState result = reader.eval((byte) '\n');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldStayInReadingHeadersState() {
        String rawRequest = "Header-Name: header-value" + '\n' + '\r';
        ReadingState state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        String crlf = "\n" + '\r';
        String rawRequest = "Content-Length: 10" + crlf + crlf;
        ReadingState state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, state);
    }

    private HeadersReader withMocks() {
        return new HeadersReader(
                new Request.Builder(),
                Mockito.mock(RequestQueue.class)
        );
    }
}
