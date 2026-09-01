package reader;

import model.HeaderKey;
import common.queue.RequestQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import model.Request;

public class HeadersReaderTest {
    private static final String CRLF = "\r" + '\n';

    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.eval((byte) 'G');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.eval((byte) '\r');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldStayInReadingHeadersState() {
        String rawRequest = "Header-Name: header-value" + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    @Test
    void shouldPassInReadingContentLengthBodyState() {
        String rawRequest = HeaderKey.CONTENT_LENGTH.getValue() + ": 10" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, state);
    }

    @Test
    void shouldPassInReadingChunkedBodyState() {
        String rawRequest = HeaderKey.TRANSFER_ENCODING.getValue() + ": chunked" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c);
        }

        Assertions.assertInstanceOf(ChunkedBodyReader.class, state);
    }

    private HeadersReader withMocks() {
        return new HeadersReader(
                Request.builder(),
                Mockito.mock(RequestQueue.class)
        );
    }
}
