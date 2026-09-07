package reader;

import model.HeaderKey;
import model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reader.dto.ReadingLifecycleEvents;
import reader.lifecycle.ChunkedBodyReader;
import reader.lifecycle.ContentLengthBodyReader;
import reader.lifecycle.HeadersReader;
import reader.lifecycle.RequestReader;

public class HeadersReaderTest {
    private static final String CRLF = "\r" + '\n';

    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldStayInReadingHeadersState() {
        String rawRequest = "Header-Name: header-value" + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    @Test
    void shouldPassInReadingContentLengthBodyState() {
        String rawRequest = HeaderKey.CONTENT_LENGTH.getValue() + ": 10" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, state);
    }

    @Test
    void shouldPassInReadingChunkedBodyState() {
        String rawRequest = HeaderKey.TRANSFER_ENCODING.getValue() + ": chunked" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        Assertions.assertInstanceOf(ChunkedBodyReader.class, state);
    }

    private HeadersReader withMocks() {
        return new HeadersReader(
                new ReadingLifecycleEvents(
                        request -> {
                        },
                        () -> {
                        },
                        () -> {
                        },
                        () -> {
                        }
                ),
                Request.builder()
        );
    }
}
