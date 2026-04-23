package router;

import model.Request;
import model.Response;

import java.util.HashMap;

public class Router {
    private final Segment root = new Segment(new HashMap<>(), new HashMap<>());

    public void add(RequestHandler requestHandler) {
        String[] path = requestHandler.getPath().split("/");
        Segment currSegment = root;

        for (String segmentName : path) {
            Segment segment = new Segment(new HashMap<>(), new HashMap<>());

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
