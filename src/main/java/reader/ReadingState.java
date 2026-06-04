package reader;

import reader.dto.RequestContext;

public abstract class ReadingState {
    protected final RequestContext context;

    protected ReadingState(RequestContext context) {
        this.context = context;
    }

    public abstract ReadingState eval(byte requestByte);
}
