package parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.Header;
import shared.exception.ResponseStatusException;
import shared.model.Status;

public class HeaderParserTest {
    private final HeaderParser headerParser = new HeaderParser();

    @Test
    void testValid() {
        Header header = headerParser.from("name: value");

        Assertions.assertEquals(new Header("name", "value"), header);
    }

    @Test
    void testMissingColon() {
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> headerParser.from("name value"));

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testInvalidHeaderName() {
        var ex = Assertions.assertThrows(ResponseStatusException.class, () -> headerParser.from("invalid name: value"));

        Assertions.assertEquals(Status.BAD_REQUEST, ex.getStatus());
    }
}
