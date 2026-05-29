package router;

import lombok.AllArgsConstructor;
import router.model.Segment;
import shared.model.Request;
import shared.model.Response;

@AllArgsConstructor
public class Router {
    private final Segment root;

    public Response handle(Request request) {
        return root
                .findByPath(request.target().url())
                .handleRequest(request);
    }
}
