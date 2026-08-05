package reader.util;

import shared.exception.ResponseStatusException;
import shared.model.Status;

public class CRLFSequenceStateTracker {
    private Boolean isLineFeed;

    public void setLineFeed() {
        if (isLineFeed != null && isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = true;
    }

    public void setCarriageReturn() {
        if (isLineFeed != null && !isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = false;
    }
}
