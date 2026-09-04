package router;

import lombok.AllArgsConstructor;
import model.*;
import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;

import java.io.ByteArrayInputStream;
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
            currSegment = currSegment.children.get(segmentName);

            if (currSegment == null) {
                return new Response(
                        Version.HTTP_1_1,
                        Status.NOT_FOUND,
                        Map.of(),
                        new ByteArrayInputStream("Couldn't find the requested path.".getBytes())
                );
            }
        }

        return Optional
                .ofNullable(currSegment.methodHandlers.get(request.getMethod()))
                .map(handler -> {
                    try {
                        return handler.handle(request);
                    } catch (Exception e) {
                        return new Response(
                                Version.HTTP_1_1,
                                Status.INTERNAL_SERVER_ERROR,
                                Map.of(),
                                new ByteArrayInputStream(e.getMessage().getBytes())
                        );
                    }
                })
                .orElse(new Response(
                        Version.HTTP_1_1,
                        Status.METHOD_NOT_ALLOWED,
                        Map.of(),
                        new ByteArrayInputStream("The requested path is not configured for this method.".getBytes())
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

            if (Method.GET.equals(command.method())) {
                currSegment.methodHandlers.put(Method.HEAD, request -> {
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
