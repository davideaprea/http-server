package reader.state;

import reader.dto.RequestContext;

public abstract class ParsingState {
    protected final RequestContext context;

    protected ParsingState(RequestContext context) {
        this.context = context;
    }

    public abstract ParsingState eval(byte requestByte);
}
