package router.model;

import router.dto.HandlerCreateCommand;
import router.exception.ConflictingRoutesException;
import shared.exception.ResponseStatusException;
import shared.model.Method;
import shared.model.Request;
import shared.model.Response;
import shared.model.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Segment {
    private final String path;
    private final Map<Method, RequestHandler> methodHandlers = new HashMap<>();
    private final Map<String, Segment> children = new HashMap<>();

    public Segment(String path) {
        this.path = path;
    }

    public Segment createOnPath(String path) {
        String[] pathSegments = path.split("/");
        Segment currSegment = this;
        StringBuilder currPath = new StringBuilder();

        for (String segmentName : pathSegments) {
            currPath.append(segmentName);

            var segmentChildren = currSegment.children;

            if (segmentChildren.containsKey(segmentName)) {
                currSegment = segmentChildren.get(segmentName);
            } else {
                var segment = new Segment(currPath.toString());

                segmentChildren.put(segmentName, segment);

                currSegment = segment;
            }
        }

        return currSegment;
    }

    public void addHandler(HandlerCreateCommand command) {
        if (methodHandlers.containsKey(command.method())) {
            throw new ConflictingRoutesException(path, command.method());
        }

        methodHandlers.put(command.method(), command.handler());
    }

    public Segment findByPath(String path) {
        String[] pathSegments = path.split("/");
        Segment currSegment = this;

        for (String segmentName : pathSegments) {
            currSegment = Optional
                    .ofNullable(children.get(segmentName))
                    .orElseThrow(() -> new ResponseStatusException(
                            "Couldn't find the requested path.",
                            Status.NOT_FOUND
                    ));
        }

        return currSegment;
    }

    public Response handleRequest(Request request) {
        return Optional
                .ofNullable(methodHandlers.get(request.method()))
                .map(handler -> handler.handle(request))
                .orElseThrow(() -> new ResponseStatusException(
                        "The requested path is not configured for this method.",
                        Status.NOT_IMPLEMENTED
                ));
    }
}
