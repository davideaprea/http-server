package io.github.davideaprea.httpserver.reader.lifecycle;

import io.github.davideaprea.httpserver.model.RequestBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.Mocks;

import java.nio.charset.StandardCharsets;

public class ChunkedBodyReaderTest {
    @Test
    void shouldParseSingleChunk() {
        String content = "0123456789";
        RequestBody requestBody = new RequestBody(() -> {
        });
        RequestReader reader = evaluateBody(
                new ChunkedBodyReader(Mocks.context(), requestBody),
                "A\r\n%s\r\n0\r\n\r\n".formatted(content)
        );
        String body = buildBody(requestBody);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
        Assertions.assertEquals(content, body);
    }

    @Test
    void shouldParseMultipleChunks() {
        RequestBody requestBody = new RequestBody(() -> {
        });
        RequestReader reader = evaluateBody(
                new ChunkedBodyReader(Mocks.context(), requestBody),
                "5\r\nhello\r\n6\r\n world\r\n0\r\n\r\n"
        );
        String body = buildBody(requestBody);

        Assertions.assertInstanceOf(RequestLineReader.class, reader);
        Assertions.assertEquals("hello world", body);
    }

    private RequestReader evaluateBody(RequestReader reader, String content) {
        RequestReader curr = reader;

        for (byte b : content.getBytes(StandardCharsets.US_ASCII)) {
            curr = reader.evalNextReader(b);
        }

        return curr;
    }

    private String buildBody(RequestBody requestBody) {
        StringBuilder body = new StringBuilder();
        int bodyByte;

        while ((bodyByte = requestBody.dequeue()) != -1) {
            body.append((char) bodyByte);
        }

        return body.toString();
    }
}
