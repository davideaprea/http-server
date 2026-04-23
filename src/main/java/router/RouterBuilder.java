package router;

public class RouterBuilder {
    private final Segment root = Segment.withDefault();

    public RouterBuilder add(RequestHandler requestHandler) {
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

        return this;
    }

    public Router build() {
        return new Router(root);
    }
}
