package io.github.davideaprea.httpserver.parser;

import io.github.davideaprea.httpserver.parser.HeaderParser;
import io.github.davideaprea.httpserver.parser.exception.BadFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.davideaprea.httpserver.parser.dto.Header;

public class HeaderParserTest {
    @Test
    void testValid() {
        Header header = HeaderParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testEmptyValue() {
        Header header = HeaderParser.from("name:");

        Assertions.assertEquals(new Header("name", ""), header);
    }

    @Test
    void testMissingNameAndValue() {
        Assertions.assertThrows(BadFormatException.class, () -> HeaderParser.from(":"));
    }

    @Test
    void testMissingColon() {
        Assertions.assertThrows(BadFormatException.class, () -> HeaderParser.from("name value"));
    }

    @Test
    void testMissingName() {
        Assertions.assertThrows(BadFormatException.class, () -> HeaderParser.from(": value"));
    }

    @Test
    void testInvalidHeaderName() {
        Assertions.assertThrows(BadFormatException.class, () -> HeaderParser.from("invalid name: value"));
    }
}
