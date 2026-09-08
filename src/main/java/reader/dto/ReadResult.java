package reader.dto;

import reader.lifecycle.RequestReader;

public record ReadResult(
        RequestReader nextReader,
        boolean canProceed
) {
}
