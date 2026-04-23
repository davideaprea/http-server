package request.parser;

import parser.HeaderParser;
import parser.dto.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import exception.MalformedHeaderException;

public class HeaderParserTest {
    private final HeaderParser headerParser = new HeaderParser();

    @Test
    void testValid() {
        Header header = headerParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testMissingColon() {
        Assertions.assertThrows(MalformedHeaderException.class, () -> headerParser.from("name value"));
    }

    @Test
    void testInvalidHeaderName() {
        Assertions.assertThrows(MalformedHeaderException.class, () -> headerParser.from("invalid name: value"));
    }
}
