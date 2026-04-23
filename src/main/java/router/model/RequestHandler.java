package router.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shared.model.Method;
import shared.model.Request;
import shared.model.Response;

@AllArgsConstructor
@Getter
public abstract class RequestHandler {
    private final Method method;
    private final String path;

    public abstract Response handle(Request request);
}
