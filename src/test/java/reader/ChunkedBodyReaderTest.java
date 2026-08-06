package reader;

import common.streaming.RequestBodyBytesQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class ChunkedBodyReaderTest {
    @Test
    void shouldParseSingleChunk() {
        String content = "0123456789";
        RequestBodyBytesQueue requestBodyBytesQueue = new RequestBodyBytesQueue();
        RequestReader reader = evaluateBody(
                new ChunkedBodyReader(requestBodyBytesQueue, null),
                "A\r\n%s\r\n0\r\n\r\n".formatted(content)
        );
        String body = buildBody(requestBodyBytesQueue);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
        Assertions.assertEquals(content, body);
    }

    @Test
    void shouldParseMultipleChunks() {
        RequestBodyBytesQueue requestBodyBytesQueue = new RequestBodyBytesQueue();
        RequestReader reader = evaluateBody(
                new ChunkedBodyReader(requestBodyBytesQueue, null),
                "5\r\nhello\r\n6\r\n world\r\n0\r\n\r\n"
        );
        String body = buildBody(requestBodyBytesQueue);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
        Assertions.assertEquals("hello world", body);
    }

    private RequestReader evaluateBody(RequestReader reader, String content) {
        RequestReader curr = reader;

        for (byte b : content.getBytes(StandardCharsets.US_ASCII)) {
            curr = reader.eval(b);
        }

        return curr;
    }

    private String buildBody(RequestBodyBytesQueue requestBodyBytesQueue) {
        StringBuilder body = new StringBuilder();
        int bodyByte;

        while ((bodyByte = requestBodyBytesQueue.dequeue()) != -1) {
            body.append((char) bodyByte);
        }

        return body.toString();
    }
}
