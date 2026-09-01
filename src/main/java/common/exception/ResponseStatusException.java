package common.exception;

import lombok.Getter;
import model.Status;

@Getter
public class ResponseStatusException extends RuntimeException {
    private final Status status;

    public ResponseStatusException(String message, Status status) {
        super(message);
        this.status = status;
    }

    public ResponseStatusException(Status status) {
        super();
        this.status = status;
    }
}
