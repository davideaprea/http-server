package request.parser;

import request.model.Method;
import request.dto.RequestLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import request.model.Version;

public class RequestLineParserTest {
    @Test
    void testValid() {
        RequestLine requestLine = RequestLineParser.from("POST /a/b/c HTTP/1.1");

        Assertions.assertEquals(new RequestLine(
                Method.POST,
                "/a/b/c",
                Version.HTTP_1_0
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

    @Test
    void testMissingPart() {
        Assertions.assertThrows(
                Exception.class,
                () -> RequestLineParser.from("POST HTTP/1.1")
        );
    }
}
