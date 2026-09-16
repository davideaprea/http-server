package reader.lifecycle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.dto.ReadingLifecycleEvents;
import reader.dto.SizeLimits;

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
                new ReadingLifecycleEvents(
                        request -> {
                        },
                        () -> {
                        },
                        () -> {
                        },
                        () -> {
                        },
                        (error) -> {
                        }
                ),
                Mockito.mock(),
                bytesNumber,
                new SizeLimits(1000, 1000)
        );
    }
}
