package request.exception;

public class MalformedHeaderException extends RuntimeException {
  public MalformedHeaderException(String rawHeader, String message) {
    super("Header with value %s is malformed. Cause: %s".formatted(rawHeader, message));
  }
}
