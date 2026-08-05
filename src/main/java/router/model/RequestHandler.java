package router.model;

import common.model.Request;
import common.model.Response;

public interface RequestHandler {
    Response handle(Request request);
}
