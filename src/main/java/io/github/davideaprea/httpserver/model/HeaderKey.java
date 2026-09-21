package io.github.davideaprea.httpserver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HeaderKey {
    TRANSFER_ENCODING("transfer-encoding"),
    CONTENT_TYPE("content-type"),
    CONTENT_LENGTH("content-length"),
    CONNECTION("connection"),
    HOST("host"),
    DATE("date");

    private final String value;
}
