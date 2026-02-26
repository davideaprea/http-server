package parsing.parser;

import parsing.model.RequestTarget;

import java.util.*;

public class RequestTargetParser {
    private RequestTargetParser() {
    }

    public static RequestTarget from(String requestTarget) {
        Map<String, List<String>> queryParams = new HashMap<>();
        int paramsStartIndex = requestTarget.indexOf('?');

        if (paramsStartIndex > -1) {
            Arrays.stream(requestTarget
                            .substring(paramsStartIndex + 1)
                            .split("&"))
                    .map(rawParam -> rawParam.split("="))
                    .forEach(pair -> {
                        queryParams.putIfAbsent(pair[0], new ArrayList<>());
                        queryParams.get(pair[0]).add(pair[1]);
                    });

            requestTarget = requestTarget.substring(0, paramsStartIndex);
        }

        return new RequestTarget(requestTarget, queryParams);
    }
}
