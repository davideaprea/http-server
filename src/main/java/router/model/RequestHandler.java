package router.model;

import shared.model.Request;
import shared.model.Response;

public interface RequestHandler {
    Response handle(Request request);
}
