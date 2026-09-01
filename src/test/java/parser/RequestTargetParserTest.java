package parser;

import common.MultiValueMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.dto.RequestTarget;

public class RequestTargetParserTest {
    @Test
    void parseValid() {
        RequestTarget requestTarget = RequestTargetParser.from("/target");

        Assertions.assertEquals(new RequestTarget("/target", new MultiValueMap<>()), requestTarget);
    }

    @Test
    void parseWithParams() {
        RequestTarget requestTarget = RequestTargetParser.from("/target?a=1&&b=2&name=John=Doe&a=3");
        Assertions.assertEquals(new RequestTarget(
                "/target",
                new MultiValueMap<String, String>()
                        .add("a", "1")
                        .add("a", "3")
                        .add("b", "2")
                        .add("name", "John=Doe")
        ), requestTarget);
    }
}
