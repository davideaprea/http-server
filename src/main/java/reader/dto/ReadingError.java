package reader.dto;

public record ReadingError(
        Exception value,
        boolean isRecoverable
) {
}
