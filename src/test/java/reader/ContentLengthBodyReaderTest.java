package reader;

import common.queue.RequestQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ContentLengthBodyReaderTest {
    @Test
    void shouldGoToRequestLineReadingAfterProcessingLastByte() {
        RequestReader reader = new ContentLengthBodyReader(
                Mockito.mock(), 1,
                Mockito.mock(RequestQueue.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void shouldRemainInSameState() {
        RequestReader reader = new ContentLengthBodyReader(
                Mockito.mock(), 2,
                Mockito.mock(RequestQueue.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    @Test
    void shouldThrowExceptionWhenReadingCompletedBody() {
        RequestReader reader = new ContentLengthBodyReader(
                Mockito.mock(), 0,
                Mockito.mock(RequestQueue.class)
        );

        Assertions.assertThrows(Exception.class, () -> reader.eval((byte) 0));
    }
}
