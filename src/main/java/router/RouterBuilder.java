package router;

import router.dto.RequestHandlerRegisterCommand;
import router.exception.ConflictingRoutesException;
import router.model.Segment;

public class RouterBuilder {
    private final Segment root = Segment.withDefault();

    public RouterBuilder add(RequestHandlerRegisterCommand command) {
        String[] path = command.path().split("/");
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

        if (currSegment.methodHandlers().containsKey(command.method())) {
            throw new ConflictingRoutesException(command.path(), command.method());
        }

        currSegment.methodHandlers().put(command.method(), command.handler());

        return this;
    }

    public Router build() {
        return new Router(root);
    }
}
