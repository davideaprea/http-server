package parser.state;

import shared.exception.ResponseStatusException;
import shared.model.Status;

public class CRLFSequenceValidator {
    private boolean isLineFeed = false;

    public void setLineFeedState() {
        if (isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = true;
    }

    public void setCarriageReturnState() {
        if (!isLineFeed) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        isLineFeed = false;
    }
}
