package router;

import model.Method;

import java.util.Map;

public record Segment(
        Map<Method, RequestHandler> methodHandlers,
        Map<String, Segment> children
) {
}
