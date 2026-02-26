package parser;

import model.RequestTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestTargetParserTest {
    @Test
    void parseValid() {
        RequestTarget requestTarget = RequestTargetParser.from("/target");

        Assertions.assertEquals(new RequestTarget("/target", new HashMap<>()), requestTarget);
    }

    @Test
    void parseWithParams() {
        RequestTarget requestTarget = RequestTargetParser.from("/target?a=1&b=2");

        Assertions.assertEquals(new RequestTarget(
                "/target",
                Map.of(
                        "a", List.of("1"),
                        "b", List.of("2")
                )
        ), requestTarget);
    }
}
