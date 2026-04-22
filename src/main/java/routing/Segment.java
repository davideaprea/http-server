package routing;

import request.model.Method;
import request.model.Request;
import response.Response;

import java.util.HashMap;
import java.util.Map;

public class Segment {
    private final Map<Method, RequestHandler> methodHandlers = new HashMap<>();
    private final Map<String, Segment> children = new HashMap<>();

    public void add(RequestHandler requestHandler) {
        String[] path = requestHandler.getPath().split("/");
        Segment currSegment = this;

        for (String segmentName : path) {
            Segment segment = new Segment();

            currSegment.children.put(segmentName, segment);

            currSegment = segment;
        }

        currSegment.methodHandlers.put(requestHandler.getMethod(), requestHandler);
    }

    public Response handle(Request request) {
        String[] path = request.target().url().split("/");
        Segment currSegment = this;

        for (String segmentName : path) {
            currSegment = children.get(segmentName);
        }

        return currSegment.methodHandlers.get(request.method()).handle(request);
    }
}
