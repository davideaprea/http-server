package router;

import model.Request;
import model.Response;

public class Router {
    private final Segment root = Segment.withDefault();

    public void add(RequestHandler requestHandler) {
        String[] path = requestHandler.getPath().split("/");
        Segment currSegment = root;

        for (String segmentName : path) {
            Segment segment = Segment.withDefault();

            currSegment.children().put(segmentName, segment);

            currSegment = segment;
        }

        currSegment.methodHandlers().put(requestHandler.getMethod(), requestHandler);
    }

    public Response handle(Request request) {
        String[] path = request.target().url().split("/");
        Segment currSegment = root;

        for (String segmentName : path) {
            currSegment = root.children().get(segmentName);
        }

        return currSegment.methodHandlers().get(request.method()).handle(request);
    }
}
