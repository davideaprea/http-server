package request.parser;

import parser.HeaderParser;
import parser.dto.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import exception.MalformedHeaderException;

public class HeaderParserTest {
    @Test
    void testValid() {
        Header header = HeaderParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testMissingColon() {
        Assertions.assertThrows(MalformedHeaderException.class, () -> HeaderParser.from("name value"));
    }

    @Test
    void testInvalidHeaderName() {
        Assertions.assertThrows(MalformedHeaderException.class, () -> HeaderParser.from("invalid name: value"));
    }
}
