package router;

import model.Method;
import model.Request;
import model.Response;

public abstract class RequestHandler {
    private final Method method;
    private final String path;

    protected RequestHandler(Method method, String path) {
        this.method = method;
        this.path = path;
    }

    public abstract Response handle(Request request);

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
