package parser;

import model.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HeaderParserTest {
    @Test
    void testValid() {
        Header header = HeaderParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testMissingColon() {
        Assertions.assertThrows(Exception.class, () -> HeaderParser.from("name value"));
    }

    @Test
    void testInvalidHeaderName() {
        Assertions.assertThrows(Exception.class, () -> HeaderParser.from("name with spaces: value"));
    }
}
