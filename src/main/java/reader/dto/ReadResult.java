package reader.dto;

import reader.lifecycle.RequestReader;

public record ReadResult(
        RequestReader nextReader,
        NextAction nextAction
) {
    public enum NextAction {
        PROCEED,
        WAIT
    }
}
