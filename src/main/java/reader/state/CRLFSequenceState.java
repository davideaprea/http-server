package reader.state;

import shared.exception.ResponseStatusException;
import shared.model.Status;

public class CRLFSequenceState {
    private boolean isLineFeed = false;

    public void setLineFeed() {
        if (isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = true;
    }

    public void setCarriageReturn() {
        if (!isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = false;
    }
}
