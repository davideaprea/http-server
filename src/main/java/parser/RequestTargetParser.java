package parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import model.Method;
import model.Version;
import parser.dto.RequestTarget;
import parser.exception.BadFormatException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestTargetParser {
    public static RequestTarget from(String rawRequestLine) {
        String[] splitRequestLine = rawRequestLine.split(" ");

        if (splitRequestLine.length != 3) {
            throw new BadFormatException("Request line is malformed.");
        }

        String requestTarget = splitRequestLine[1];

        if (!requestTarget.startsWith("/")) {
            throw new BadFormatException("Request URI must start with a backslash.");
        }

        int paramsStartIndex = requestTarget.indexOf('?');
        String path = paramsStartIndex > -1 ? requestTarget.substring(0, paramsStartIndex) : requestTarget;
        Map<String, List<String>> queryParams = new HashMap<>();

        if (paramsStartIndex > -1 && paramsStartIndex + 1 < requestTarget.length()) {
            String rawQuery = requestTarget.substring(paramsStartIndex + 1);
            queryParams = parseQueryParams(rawQuery);
        }

        return new RequestTarget(
                parseMethod(splitRequestLine[0]),
                parseVersion(splitRequestLine[2]),
                path,
                queryParams
        );
    }

    private static Version parseVersion(String value) {
        try {
            return Version.fromValue(value);
        } catch (NoSuchElementException e) {
            throw new BadFormatException("The requested version %s is not supported.".formatted(value));
        }
    }

    private static Method parseMethod(String value) {
        try {
            return Method.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadFormatException("Request method %s is not valid.".formatted(value));
        }
    }

    private static Map<String, List<String>> parseQueryParams(String rawQuery) {
        if (rawQuery.chars().anyMatch(Character::isWhitespace)) {
            throw new BadFormatException("Found whitespace character in raw query params.");
        }

        Map<String, List<String>> queryParams = new HashMap<>();

        Arrays.stream(rawQuery.split("&"))
                .filter(s -> !s.isEmpty())
                .map(rawParam -> {
                    String[] pair = rawParam.split("=", 2);

                    if (pair.length != 2) {
                        throw new BadFormatException("Missing param %s value.".formatted(pair[0]));
                    }

                    return pair;
                })
                .forEach(pair -> {
                    String rawKey = pair[0];
                    String rawValue = pair[1];
                    String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);

                    queryParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                });

        return queryParams;
    }
}
