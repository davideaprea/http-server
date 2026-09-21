package io.github.davideaprea.httpserver.parser.dto;

import io.github.davideaprea.httpserver.model.Method;
import io.github.davideaprea.httpserver.model.Version;

import java.util.List;
import java.util.Map;

public record RequestTarget(
        Method method,
        Version version,
        String url,
        Map<String, List<String>> queryParams
) {
}
