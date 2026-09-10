package reader.lifecycle;

import common.MalformedRequestException;
import model.HeaderKey;
import model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

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

    @Test
    void testNegativeContentLength() {
        Assertions.assertThrows(MalformedRequestException.class, () -> {
            String rawRequest = HeaderKey.CONTENT_LENGTH.getValue() + ": -1" + CRLF + CRLF;
            RequestReader state = withMocks();

            for (int i = 0; i < rawRequest.length(); i++) {
                char c = rawRequest.charAt(i);
                state = state.eval((byte) c).nextReader();
            }
        });
    }

    @Test
    void testDoubleContentLength() {
        Assertions.assertThrows(MalformedRequestException.class, () -> {
            String contentLengthHeader = HeaderKey.CONTENT_LENGTH.getValue() + ": 1";
            String rawRequest = contentLengthHeader + CRLF + contentLengthHeader + CRLF + CRLF;
            RequestReader state = withMocks();

            for (int i = 0; i < rawRequest.length(); i++) {
                char c = rawRequest.charAt(i);
                state = state.eval((byte) c).nextReader();
            }
        });
    }

    @Test
    void testTransferEncodingAndContentLengthPresentAtTheSameTime() {
        Assertions.assertThrows(MalformedRequestException.class, () -> {
            String contentLengthHeader = HeaderKey.CONTENT_LENGTH.getValue() + ": 1";
            String transferEncodingHeader = HeaderKey.TRANSFER_ENCODING.getValue() + ": chunked";
            String rawRequest = contentLengthHeader + CRLF + transferEncodingHeader + CRLF + CRLF;
            RequestReader state = withMocks();

            for (int i = 0; i < rawRequest.length(); i++) {
                char c = rawRequest.charAt(i);
                state = state.eval((byte) c).nextReader();
            }
        });
    }

    @Test
    void testZeroContentLength() {
        String contentLengthHeader = HeaderKey.CONTENT_LENGTH.getValue() + ": 0" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < contentLengthHeader.length(); i++) {
            char c = contentLengthHeader.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        Assertions.assertInstanceOf(RequestLineReader.class, state);
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
                Request.builder(),
                new SizeLimits(1000, 1000),
                1000
        );
    }
}
