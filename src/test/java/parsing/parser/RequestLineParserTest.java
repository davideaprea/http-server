package parsing.parser;

import parsing.model.Method;
import parsing.dto.RequestLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestLineParserTest {
    @Test
    void testValid() {
        RequestLine requestLine = RequestLineParser.from("POST /a/b/c HTTP/1.1");

        Assertions.assertEquals(new RequestLine(
                Method.POST,
                "/a/b/c",
                "HTTP/1.1"
        ), requestLine);
    }

    @Test
    void testInvalidMethod() {
        Assertions.assertThrows(
                Exception.class,
                () -> RequestLineParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );
    }

    @Test
    void testInvalidVersion() {
        Assertions.assertThrows(
                Exception.class,
                () -> RequestLineParser.from("POST /a/b/c INVALID-VERSION")
        );
    }
}
