package reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;
import shared.RequestBodyStream;

public class ContentLengthBodyReaderTest {
    @Test
    void a() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(new RequestBodyStream(), 1),
                Mockito.mock(RequestContext.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }

    @Test
    void b() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(new RequestBodyStream(), 2),
                Mockito.mock(RequestContext.class)
        ).eval((byte) 0);

        Assertions.assertInstanceOf(ContentLengthBodyReader.class, reader);
    }

    @Test
    void c() {
        ReadingState reader = new ContentLengthBodyReader(
                new ContentLengthRequest(new RequestBodyStream(), 0),
                Mockito.mock(RequestContext.class)
        );

        Assertions.assertThrows(Exception.class, () -> reader.eval((byte) 0));
    }
}
