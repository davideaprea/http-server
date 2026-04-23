package router.model;

import shared.model.Method;

import java.util.HashMap;
import java.util.Map;

public record Segment(
        Map<Method, RequestHandler> methodHandlers,
        Map<String, Segment> children
) {
    public static Segment withDefault() {
        return new Segment(new HashMap<>(), new HashMap<>());
    }
}
