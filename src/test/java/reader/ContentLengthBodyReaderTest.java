package reader;

import common.queue.RequestQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ContentLengthBodyReaderTest {
    @Test
    void shouldGoToRequestLineReadingAfterProcessingLastByte() {
        RequestReader reader = newContentLengthBodyReader(1).eval((byte) 0);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void shouldRemainInSameState() {
        RequestReader reader = newContentLengthBodyReader(2).eval((byte) 0);

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    @Test
    void shouldThrowExceptionWhenReadingCompletedBody() {
        RequestReader reader = newContentLengthBodyReader(0);

        Assertions.assertThrows(Exception.class, () -> reader.eval((byte) 0));
    }

    private ContentLengthBodyReader newContentLengthBodyReader(int bytesNumber) {
        return new ContentLengthBodyReader(
                bytesNumber,
                Mockito.mock(RequestQueue.class),
                Mockito.mock()
        );
    }
}
