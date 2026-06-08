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
        );

        Assertions.assertSame(reader, (reader = reader.eval(Mockito.anyByte())));
        Assertions.assertInstanceOf(RequestLineReader.class, reader);
    }
}
