package parsing.parser;

import parsing.dto.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parsing.exception.MalformedHeaderException;

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
        Assertions.assertThrows(MalformedHeaderException.class, () -> HeaderParser.from("name with spaces: value"));
    }
}
