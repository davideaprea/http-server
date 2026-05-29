package router.model;

import lombok.AllArgsConstructor;
import router.dto.HandlerCreateCommand;
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

    public static final class Builder {
        private final Segment root = new Segment("");

        public Builder add(HandlerCreateCommand command) {
            Segment currSegment = root.createOnPath(command.path());

            currSegment.addHandler(command);

            return this;
        }

        public Router build() {
            return new Router(root);
        }
    }
}
