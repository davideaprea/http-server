package request.parser;

import request.model.RequestTarget;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestTargetParser {
    private RequestTargetParser() {
    }

    public static RequestTarget from(String requestTarget) {
        int paramsStartIndex = requestTarget.indexOf('?');
        String path = paramsStartIndex > -1 ? requestTarget.substring(0, paramsStartIndex) : requestTarget;
        Map<String, List<String>> queryParams = new HashMap<>();

        if (paramsStartIndex > -1 && paramsStartIndex + 1 < requestTarget.length()) {
            String rawQuery = requestTarget.substring(paramsStartIndex + 1);

            Arrays.stream(rawQuery.split("&"))
                    .filter(s -> !s.isEmpty())
                    .forEach(rawParam -> {
                        String[] pair = rawParam.split("=", 2);
                        String rawKey = pair[0];
                        String rawValue = pair.length == 2 ? pair[1] : "";
                        String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);

                        queryParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                    });
        }

        return new RequestTarget(path, queryParams);
    }
}
