package router.model;

import lombok.AllArgsConstructor;
import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;
import common.exception.ResponseStatusException;
import common.model.Method;
import common.model.Request;
import common.model.Response;
import common.model.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class Router {
    private final Segment root;

    public Response handle(Request request) {
        String[] pathSegments = request.target().url().split("/");
        Segment currSegment = root;

        for (String segmentName : pathSegments) {
            currSegment = Optional
                    .ofNullable(currSegment.children.get(segmentName))
                    .orElseThrow(() -> new ResponseStatusException(
                            "Couldn't find the requested path.",
                            Status.NOT_FOUND
                    ));
        }

        return Optional
                .ofNullable(currSegment.methodHandlers.get(request.method()))
                .map(handler -> handler.handle(request))
                .orElseThrow(() -> new ResponseStatusException(
                        "The requested path is not configured for this method.",
                        Status.NOT_IMPLEMENTED
                ));
    }

    public static final class Builder {
        private final Segment root = Segment.withDefaults();

        public Builder add(HandlerCreateCommand command) {
            String[] pathSegments = command.path().split("/");
            Segment currSegment = root;

            for (String segmentName : pathSegments) {
                var segmentChildren = currSegment.children;

                if (segmentChildren.containsKey(segmentName)) {
                    currSegment = segmentChildren.get(segmentName);
                } else {
                    var segment = Segment.withDefaults();

                    segmentChildren.put(segmentName, segment);

                    currSegment = segment;
                }
            }

            if (currSegment.methodHandlers.containsKey(command.method())) {
                throw new ConflictingRoutesException(command.path(), command.method());
            }

            currSegment.methodHandlers.put(command.method(), command.handler());

            return this;
        }

        public Router build() {
            return new Router(root);
        }
    }

    public record Segment(
            Map<Method, RequestHandler> methodHandlers,
            Map<String, Segment> children
    ) {
        public static Segment withDefaults() {
            return new Segment(new HashMap<>(), new HashMap<>());
        }
    }
}
