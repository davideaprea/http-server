package parser;

import model.Method;
import model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.RequestTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class RequestTargetParserTest {
    @Test
    void testValid() {
        RequestTarget requestLine = RequestTargetParser.from("POST /a/b/c HTTP/1.1");

        Assertions.assertEquals(new RequestTarget(
                Method.POST,
                Version.HTTP_1_1,
                "/a/b/c",
                new HashMap<>()
        ), requestLine);
    }

    @Test
    void testInvalidMethod() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RequestTargetParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );
    }

    @Test
    void testInvalidVersion() {
        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> RequestTargetParser.from("POST /a/b/c INVALID-VERSION")
        );
    }

    @Test
    void testMissingPart() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RequestTargetParser.from("POST HTTP/1.1")
        );
    }

    @Test
    void parseValid() {
        RequestTarget requestTarget = RequestTargetParser.from("GET /target HTTP/1.1");

        Assertions.assertEquals(new RequestTarget(
                Method.GET,
                Version.HTTP_1_1,
                "/target",
                new HashMap<>()
        ), requestTarget);
    }

    @Test
    void parseInvalid() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
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
                Map.of(
                        "a", List.of("1", "3"),
                        "b", List.of("2"),
                        "name", List.of("John=Doe")
                )
        ), requestTarget);
    }

    @Test
    void parseWithEmptyParams() {
        Assertions.assertEquals(
                new RequestTarget(
                        Method.GET,
                        Version.HTTP_1_1,
                        "/",
                        new HashMap<>()
                ),
                RequestTargetParser.from("GET /? HTTP/1.1")
        );
    }
}
