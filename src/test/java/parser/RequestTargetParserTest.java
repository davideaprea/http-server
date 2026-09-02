package parser;

import common.MultiValueMap;
import common.exception.ResponseStatusException;
import model.Method;
import model.Status;
import model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.RequestTarget;

public class RequestTargetParserTest {
    @Test
    void testValid() {
        RequestTarget requestLine = RequestTargetParser.from("POST /a/b/c HTTP/1.1");

        Assertions.assertEquals(new RequestTarget(
                Method.POST,
                Version.HTTP_1_1,
                "/a/b/c",
                new MultiValueMap<>()
        ), requestLine);
    }

    @Test
    void testInvalidMethod() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestTargetParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testInvalidVersion() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestTargetParser.from("POST /a/b/c INVALID-VERSION")
        );

        Assertions.assertEquals(Status.VERSION_NOT_SUPPORTED, ex.getStatus());
    }

    @Test
    void testMissingPart() {
        var ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestTargetParser.from("POST HTTP/1.1")
        );

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void parseValid() {
        RequestTarget requestTarget = RequestTargetParser.from("GET /target HTTP/1.1");

        Assertions.assertEquals(new RequestTarget(
                Method.GET,
                Version.HTTP_1_1,
                "/target",
                new MultiValueMap<>()
        ), requestTarget);
    }

    @Test
    void parseInvalid() {
        Assertions.assertThrows(
                ResponseStatusException.class,
                () -> RequestTargetParser.from("target")
        );
    }

    @Test
    void parseWithParams() {
        RequestTarget requestTarget = RequestTargetParser.from("GET /target?a=1&&b=2&name=John=Doe&a=3 HTTP/1.1");
        Assertions.assertEquals(new RequestTarget(
                Method.GET,
                Version.HTTP_1_1,
                "/target",
                new MultiValueMap<String, String>()
                        .add("a", "1")
                        .add("a", "3")
                        .add("b", "2")
                        .add("name", "John=Doe")
        ), requestTarget);
    }

    @Test
    void parseWithEmptyParams() {
        Assertions.assertEquals(
                new RequestTarget(
                        Method.GET,
                        Version.HTTP_1_1,
                        "/",
                        new MultiValueMap<>()
                ),
                RequestTargetParser.from("GET /? HTTP/1.1")
        );
    }
}
