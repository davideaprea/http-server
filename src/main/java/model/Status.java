package model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Status {
    OK(200, "OK");

    private final int code;
    private final String name;
}
