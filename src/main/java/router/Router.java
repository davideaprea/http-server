package router;

import lombok.AllArgsConstructor;
import model.Request;
import model.Response;

import java.util.Optional;

@AllArgsConstructor
public class Router {
    private final Segment root;

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
