package shared.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HeaderKey {
    CONTENT_LENGTH("Content_Length");

    private final String value;
}
