package parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.RequestLine;
import common.exception.ResponseStatusException;
import model.Method;
import model.Status;
import model.Version;

public class RequestLineParserTest {
    @Test
    void testValid() {
        RequestLine requestLine = RequestLineParser.from("POST /a/b/c HTTP/1.1");

        Assertions.assertEquals(new RequestLine(
                Method.POST,
                "/a/b/c",
                Version.HTTP_1_1
        ), requestLine);
    }

    @Test
    void testInvalidMethod() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestLineParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testInvalidVersion() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestLineParser.from("POST /a/b/c INVALID-VERSION")
        );

        Assertions.assertEquals(Status.VERSION_NOT_SUPPORTED, ex.getStatus());
    }

    @Test
    void testMissingPart() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestLineParser.from("POST HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }
}
