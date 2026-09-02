package reader;

public record ReadResult(
        RequestReader nextReader,
        NextAction nextAction
) {
    public enum NextAction {
        PROCEED,
        WAIT
    }
}
