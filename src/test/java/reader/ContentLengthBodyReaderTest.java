package reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.dto.ContentLengthRequest;
import common.streaming.RequestQueue;

public class ContentLengthBodyReaderTest {
    @Test
    void a() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(Mockito.mock(), 1),
                Mockito.mock(RequestQueue.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void b() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(Mockito.mock(), 2),
                Mockito.mock(RequestQueue.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    @Test
    void c() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(Mockito.mock(), 0),
                Mockito.mock(RequestQueue.class)
        );

        Assertions.assertThrows(Exception.class, () -> reader.eval((byte) 0));
    }
}
