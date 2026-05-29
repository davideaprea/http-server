package router;

import router.dto.HandlerCreateCommand;
import router.model.Segment;

public class RouterBuilder {
    private final Segment root = new Segment("");

    public RouterBuilder add(HandlerCreateCommand command) {
        Segment currSegment = root.createOnPath(command.path());

        currSegment.addHandler(command);

        return this;
    }

    public Router build() {
        return new Router(root);
    }
}
