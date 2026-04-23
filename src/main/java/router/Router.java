package router;

import model.Request;
import model.Response;

import java.util.Optional;

public class Router {
    private final Segment root = Segment.withDefault();

    public void add(RequestHandler requestHandler) {
        String[] path = requestHandler.getPath().split("/");
        Segment currSegment = root;

        for (String segmentName : path) {
            var segmentChildren = currSegment.children();

            if (segmentChildren.containsKey(segmentName)) {
                currSegment = segmentChildren.get(segmentName);
            } else {
                var segment = Segment.withDefault();

                segmentChildren.put(segmentName, segment);

                currSegment = segment;
            }
        }

        if (currSegment.methodHandlers().containsKey(requestHandler.getMethod())) {
            throw new IllegalStateException();
        }

        currSegment.methodHandlers().put(requestHandler.getMethod(), requestHandler);
    }

    public Response handle(Request request) {
        String[] path = request.target().url().split("/");
        Segment currSegment = root;

        for (String segmentName : path) {
            currSegment = Optional
                    .ofNullable(currSegment.children().get(segmentName))
                    .orElseThrow();
        }

        RequestHandler handler = Optional
                .ofNullable(currSegment.methodHandlers().get(request.method()))
                .orElseThrow();

        return handler.handle(request);
    }
}
