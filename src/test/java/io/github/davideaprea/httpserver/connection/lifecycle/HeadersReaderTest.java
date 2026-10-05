package io.github.davideaprea.httpserver.connection.lifecycle;

import io.github.davideaprea.httpserver.model.HeaderKey;
import io.github.davideaprea.httpserver.model.Request;
import io.github.davideaprea.httpserver.connection.exception.MalformedRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.Mocks;

public class HeadersReaderTest {
    private static final String CRLF = "\r" + '\n';
    private static final String rawMandatoryHeaders = HeaderKey.HOST.getValue() + ": name" + CRLF;

    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.evalNextReader((byte) 'G');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        HeadersReader reader = withMocks();
        RequestReader result = reader.evalNextReader((byte) '\r');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldStayInReadingHeadersState() {
        String rawRequest = "Header-Name: header-value" + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.evalNextReader((byte) c);
        }

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }

    @Test
    void shouldPassInReadingContentLengthBodyState() {
        String rawRequest = rawMandatoryHeaders + HeaderKey.CONTENT_LENGTH.getValue() + ": 10" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.evalNextReader((byte) c);
        }

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, state);
    }

    @Test
    void shouldPassInReadingChunkedBodyState() {
        String rawRequest = rawMandatoryHeaders + HeaderKey.TRANSFER_ENCODING.getValue() + ": chunked" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.evalNextReader((byte) c);
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
                state = state.evalNextReader((byte) c);
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
                state = state.evalNextReader((byte) c);
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
                state = state.evalNextReader((byte) c);
            }
        });
    }

    @Test
    void testZeroContentLength() {
        String contentLengthHeader = rawMandatoryHeaders + HeaderKey.CONTENT_LENGTH.getValue() + ": 0" + CRLF + CRLF;
        RequestReader state = withMocks();

        for (int i = 0; i < contentLengthHeader.length(); i++) {
            char c = contentLengthHeader.charAt(i);
            state = state.evalNextReader((byte) c);
        }

        Assertions.assertInstanceOf(RequestLineReader.class, state);
    }

    private HeadersReader withMocks() {
        return new HeadersReader(
                Mocks.clientChannel(),
                Request.builder(),
                1000
        );
    }
}
