package parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.Header;

public class HeaderParserTest {
    @Test
    void testValid() {
        Header header = HeaderParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testMissingColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> HeaderParser.from("name value"));
    }

    @Test
    void testMissingName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> HeaderParser.from(": value"));
    }

    @Test
    void testInvalidHeaderName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> HeaderParser.from("invalid name: value"));
    }
}
