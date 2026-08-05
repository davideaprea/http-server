package common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.NoSuchElementException;

@Getter
@AllArgsConstructor
public enum Version {
    HTTP_1_1("HTTP/1.1");

    private final String value;

    public static Version fromValue(String value) {
        for (Version v : values()) {
            if (v.value.equalsIgnoreCase(value)) {
                return v;
            }
        }

        throw new NoSuchElementException("Version not supported.");
    }
}
