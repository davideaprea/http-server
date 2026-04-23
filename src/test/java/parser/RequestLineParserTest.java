package parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.RequestLine;
import shared.exception.ResponseStatusException;
import shared.model.Method;
import shared.model.Status;
import shared.model.Version;

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
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> requestLineParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testInvalidVersion() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> requestLineParser.from("POST /a/b/c INVALID-VERSION")
        );

        Assertions.assertEquals(Status.VERSION_NOT_SUPPORTED, ex.getStatus());
    }

    @Test
    void testMissingPart() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> requestLineParser.from("POST HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }
}
