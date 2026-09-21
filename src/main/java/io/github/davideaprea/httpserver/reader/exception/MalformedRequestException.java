package io.github.davideaprea.httpserver.reader.exception;

public class MalformedRequestException extends RuntimeException {
    public MalformedRequestException(String message) {
        super(message);
    }
}
