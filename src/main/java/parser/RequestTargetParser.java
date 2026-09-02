package parser;

import common.MultiValueMap;
import common.exception.ResponseStatusException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import model.Status;
import parser.dto.RequestTarget;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestTargetParser {
    public static RequestTarget from(String requestTarget) {
        if (!requestTarget.startsWith("/")) {
            throw new ResponseStatusException("Request URI must start with a backslash.", Status.BAD_REQUEST);
        }

        int paramsStartIndex = requestTarget.indexOf('?');
        String path = paramsStartIndex > -1 ? requestTarget.substring(0, paramsStartIndex) : requestTarget;
        MultiValueMap<String, String> queryParams = new MultiValueMap<>();

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

                        queryParams.add(key, value);
                    });
        }

        return new RequestTarget(path, queryParams);
    }
}
