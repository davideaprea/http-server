package router;

import lombok.AllArgsConstructor;
import router.model.RequestHandler;
import router.model.Segment;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Response;
import shared.model.Status;

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
                    .orElseThrow(() -> new ResponseStatusException(
                            "Couldn't find the requested path.",
                            Status.NOT_FOUND
                    ));
        }

        RequestHandler handler = Optional
                .ofNullable(currSegment.methodHandlers().get(request.method()))
                .orElseThrow(() -> new ResponseStatusException(
                        "The requested path is not configured for this method.",
                        Status.NOT_IMPLEMENTED
                ));

        return handler.handle(request);
    }
}
