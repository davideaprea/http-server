package common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HeaderKey {
    TRANSFER_ENCODING("transfer-encoding"),
    CONTENT_TYPE("content-type"),
    CONTENT_LENGTH("content-length");

    private final String value;
}
