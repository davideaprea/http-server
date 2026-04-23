package parser;

import parser.RequestLineParser;
import model.Method;
import parser.dto.RequestLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import model.Version;

public class RequestLineParserTest {
    private final RequestLineParser requestLineParser = new RequestLineParser();

    @Test
    void testValid() {
        RequestLine requestLine = requestLineParser.from("POST /a/b/c HTTP/1.1");

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
                () -> requestLineParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );
    }

    @Test
    void testInvalidVersion() {
        Assertions.assertThrows(
                Exception.class,
                () -> requestLineParser.from("POST /a/b/c INVALID-VERSION")
        );
    }

    @Test
    void testMissingPart() {
        Assertions.assertThrows(
                Exception.class,
                () -> requestLineParser.from("POST HTTP/1.1")
        );
    }
}
