package router.exception;

import model.Method;

public class ConflictingRoutesException extends RuntimeException {
    public ConflictingRoutesException(String path, Method method) {
        super("Found conflicting handlers on path %s and method %s.".formatted(
                path, method.name()
        ));
    }
}
