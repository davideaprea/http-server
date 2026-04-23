package shared.exception;

import lombok.Getter;
import shared.model.Status;

@Getter
public class ResponseStatusException extends RuntimeException {
    private final Status status;

    public ResponseStatusException(String message, Status status) {
        super(message);
        this.status = status;
    }
}
