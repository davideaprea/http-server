package shared.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Status {
    NOT_IMPLEMENTED(501, "NOT IMPLEMENTED"),
    NOT_FOUND(404, "NOT FOUND"),
    VERSION_NOT_SUPPORTED(505, "VERSION NOT SUPPORTED"),
    BAD_REQUEST(400, "BAD REQUEST"),
    OK(200, "OK");

    private final int code;
    private final String name;
}
