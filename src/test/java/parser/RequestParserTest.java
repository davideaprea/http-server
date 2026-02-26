package parser;

import model.Method;
import model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RequestParserTest {
    @Test
    void testValid() throws IOException {
        String rawRequest = """
                POST /target?a=1&b=2 HTTP/1.1
                Host: example.com
                Content-Type: application/json
                Content-Length: 49
                
                {"key":"value"}
                """;
        Request parsedRequest = RequestParser.from(new ByteArrayInputStream(rawRequest.getBytes()));

        Assertions.assertEquals(Method.POST, parsedRequest.requestLine().method());
        Assertions.assertEquals("HTTP/1.1", parsedRequest.requestLine().version());
        Assertions.assertEquals(
                Map.of(
                        "Host", List.of("example.com"),
                        "Content-Type", List.of("application/json"),
                        "Content-Length", List.of("49")
                ),
                parsedRequest.headers()
        );
    }
}
