package io.github.davideaprea.httpserver.parser;

import io.github.davideaprea.httpserver.parser.exception.BadFormatException;
import io.github.davideaprea.httpserver.model.Method;
import io.github.davideaprea.httpserver.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.davideaprea.httpserver.parser.dto.RequestTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestTargetParserTest {
    @Test
    void testInvalidMethod() {
        Assertions.assertThrows(
                BadFormatException.class,
                () -> RequestTargetParser.from("INVALID-METHOD /a/b/c HTTP/1.1")
        );
    }

    @Test
    void testInvalidVersion() {
        Assertions.assertThrows(
                BadFormatException.class,
                () -> RequestTargetParser.from("POST /a/b/c INVALID-VERSION")
        );
    }

    @Test
    void testMissingPart() {
        Assertions.assertThrows(
                BadFormatException.class,
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
                BadFormatException.class,
                () -> RequestTargetParser.from("GET target HTTP/1.1")
        );
    }

    @Test
    void parseWhitespaceCharsInParams() {
        Assertions.assertThrows(
                BadFormatException.class,
                () -> RequestTargetParser.from("GET /target?a=\t&b=2 HTTP/1.1")
        );
    }

    @Test
    void parseWithParams() {
        RequestTarget requestTarget = RequestTargetParser.from("GET /target?a=1&&b=2&name=John=Doe&a=3&c= HTTP/1.1");
        Assertions.assertEquals(new RequestTarget(
                Method.GET,
                Version.HTTP_1_1,
                "/target",
                Map.of(
                        "a", List.of("1", "3"),
                        "b", List.of("2"),
                        "name", List.of("John=Doe"),
                        "c", List.of("")
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

    @Test
    void parseMissingKeyValueSeparatorInParams() {
        Assertions.assertThrows(
                BadFormatException.class,
                () -> RequestTargetParser.from("GET /target?a=1&b HTTP/1.1")
        );
    }

    @Test
    void parsePercentEncodedTarget() {
        Assertions.assertEquals(
                new RequestTarget(
                        Method.GET,
                        Version.HTTP_1_1,
                        "/joe cafè",
                        new HashMap<>()
                ),
                RequestTargetParser.from("GET /joe%20caf%C3%A8 HTTP/1.1")
        );
    }
}
