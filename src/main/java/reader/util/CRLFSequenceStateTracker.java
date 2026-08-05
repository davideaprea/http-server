package reader.util;

import common.exception.ResponseStatusException;
import common.model.Status;

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
