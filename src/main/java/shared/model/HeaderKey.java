package shared.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HeaderKey {
    TRANSFER_ENCODING("Transfer-Encoding"),
    CONTENT_TYPE("Content-Type"),
    CONTENT_LENGTH("Content-Length");

    private final String value;
}
