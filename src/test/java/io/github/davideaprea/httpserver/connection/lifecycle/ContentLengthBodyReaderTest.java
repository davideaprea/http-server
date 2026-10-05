package io.github.davideaprea.httpserver.connection.lifecycle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import util.Mocks;

public class ContentLengthBodyReaderTest {
    @Test
    void shouldGoToRequestLineReadingAfterProcessingLastByte() {
        RequestReader reader = newContentLengthBodyReader(1).evalNextReader((byte) 0);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void shouldRemainInSameState() {
        RequestReader reader = newContentLengthBodyReader(2).evalNextReader((byte) 0);

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    private ContentLengthBodyReader newContentLengthBodyReader(int bytesNumber) {
        return new ContentLengthBodyReader(
                Mocks.clientChannel(),
                Mockito.mock(),
                bytesNumber
        );
    }
}
