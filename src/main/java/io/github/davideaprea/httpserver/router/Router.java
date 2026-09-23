package io.github.davideaprea.httpserver.router;

import io.github.davideaprea.httpserver.model.*;
import io.github.davideaprea.httpserver.router.dto.HandlerCreateCommand;
import io.github.davideaprea.httpserver.router.exception.ConflictingRoutesException;
import lombok.AllArgsConstructor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class Router {
    private final Segment root;

    /**
     * Handles an HTTP request by finding the handler associated to
     * the given {@link Request#getUrl()} and {@link Request#getMethod()}
     *
     * @param request the HTTP request to handle
     * @return the response produced by the matching handler, or a response with
     * status {@link Status#NOT_FOUND} if the requested path does not exist,
     * {@link Status#METHOD_NOT_ALLOWED} if the path does not support the requested method
     * or {@link Status#INTERNAL_SERVER_ERROR} if the handler raised an unknown exception
     */
    public Response handle(Request request) {
        String[] pathSegments = request.getUrl().substring(1).split("/");
        Segment currSegment = root;

        for (String segmentName : pathSegments) {
            currSegment = currSegment.children.get(segmentName);

            if (currSegment == null) {
                return Response.textResponse("Couldn't find the requested path.", Status.NOT_FOUND);
            }
        }

        return Optional
                .ofNullable(currSegment.methodHandlers.get(request.getMethod()))
                .map(handler -> {
                    try {
                        Response response = handler.handle(request);

                        if (
                                !response.headers().containsKey(HeaderKey.CONTENT_LENGTH.getValue()) &&
                                        !response.headers().containsKey(HeaderKey.TRANSFER_ENCODING.getValue())
                        ) {
                            response.headers().put(HeaderKey.TRANSFER_ENCODING.getValue(), "chunked");
                        }

                        return response;
                    } catch (Exception e) {
                        return Response.textResponse(
                                Status.INTERNAL_SERVER_ERROR.getName(),
                                Status.INTERNAL_SERVER_ERROR
                        );
                    }
                })
                .orElse(Response.textResponse(
                        "The requested path is not configured for this method.",
                        Status.METHOD_NOT_ALLOWED
                ));
    }

    public static final class Builder {
        private final Segment root = Segment.withDefaults();

        /**
         * Adds a handler with a specific path to the router.
         *
         * <p>If a {@link Method#GET} handler is added, a corresponding
         * {@link Method#HEAD} handler is automatically registered.</p>
         *
         * @param command the command containing the route path, HTTP method and
         *                request handler
         * @return this builder
         * @throws ConflictingRoutesException if a handler for the same path and
         *                                    method has already been registered
         */
        public Builder add(HandlerCreateCommand command) {
            String path;

            if (command.path().startsWith("/")) {
                path = command.path().substring(1);
            } else {
                path = command.path();
            }

            String[] pathSegments = path.split("/");
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

            if (Method.GET.equals(command.method()) && !currSegment.methodHandlers.containsKey(Method.HEAD)) {
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
