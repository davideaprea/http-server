package io.github.davideaprea.httpserver.reader.dto;

public record ReadingError(
        Exception value,
        boolean isRecoverable
) {
}
