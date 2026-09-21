package io.github.davideaprea.httpserver.reader.dto;

import io.github.davideaprea.httpserver.reader.lifecycle.RequestReader;

public record ReadResult(
        RequestReader nextReader,
        boolean canProceed
) {
}
