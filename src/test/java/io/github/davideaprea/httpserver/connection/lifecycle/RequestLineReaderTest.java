package io.github.davideaprea.httpserver.connection.lifecycle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.Mocks;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mocks.clientChannel());
        RequestReader result = reader.evalNextReader((byte) 'G');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        RequestLineReader reader = new RequestLineReader(Mocks.clientChannel());
        RequestReader result = reader.evalNextReader((byte) '\r');

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = new RequestLineReader(Mocks.clientChannel());

        for (int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.evalNextReader((byte) c);
        }

        state = state.evalNextReader((byte) '\r');
        state = state.evalNextReader((byte) '\n');

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }
}
