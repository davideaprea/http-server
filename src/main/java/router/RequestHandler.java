package router;

import lombok.AllArgsConstructor;
import lombok.Getter;
import model.Method;
import model.Request;
import model.Response;

@AllArgsConstructor
@Getter
public abstract class RequestHandler {
    private final Method method;
    private final String path;

    public abstract Response handle(Request request);
}
