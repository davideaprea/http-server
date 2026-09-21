package io.github.davideaprea.httpserver.router;

import io.github.davideaprea.httpserver.model.Request;
import io.github.davideaprea.httpserver.model.Response;

public interface RequestHandler {
    Response handle(Request request) throws InterruptedException;
}
