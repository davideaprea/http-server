package reader;

import client.ClientRequestsQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.lifecycle.ContentLengthBodyReader;
import reader.lifecycle.RequestLineReader;
import reader.lifecycle.RequestReader;

public class ContentLengthBodyReaderTest {
    @Test
    void shouldGoToRequestLineReadingAfterProcessingLastByte() {
        RequestReader reader = newContentLengthBodyReader(1).eval((byte) 0).nextReader();

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void shouldRemainInSameState() {
        RequestReader reader = newContentLengthBodyReader(2).eval((byte) 0).nextReader();

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    private ContentLengthBodyReader newContentLengthBodyReader(int bytesNumber) {
        return new ContentLengthBodyReader(
                Mockito.mock(ClientRequestsQueue.class),
                () -> {
                },
                Mockito.mock(),
                Mockito.mock(),
                bytesNumber
        );
    }
}
