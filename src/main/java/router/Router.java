package router;

import common.exception.ResponseStatusException;
import lombok.AllArgsConstructor;
import model.Method;
import model.Request;
import model.Response;
import model.Status;
import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class Router {
    private final Segment root;

    public Response handle(Request request) {
        String[] pathSegments = request.getUrl().split("/");
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
                .ofNullable(currSegment.methodHandlers.get(request.getMethod()))
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
                currSegment = segmentChildren.computeIfAbsent(segmentName, k -> Segment.withDefaults());
            }

            currSegment.methodHandlers.compute(command.method(), (k, v) -> {
                if (v != null) {
                    throw new ConflictingRoutesException(command.path(), command.method());
                }

                return command.handler();
            });

            if (Method.HEAD.equals(command.method())) {
                currSegment.methodHandlers.put(command.method(), request -> {
                    Response response = command.handler().handle(request);

                    return new Response(
                            response.version(),
                            response.status(),
                            response.headers(),
                            InputStream.nullInputStream()
                    );
                });
            }

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
