package router;

import model.Request;
import model.Response;

public interface RequestHandler {
    Response handle(Request request) throws InterruptedException;
}
