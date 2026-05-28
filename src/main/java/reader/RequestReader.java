package reader;

import reader.state.ParsingState;

public class RequestReader {
    private ParsingState parsingState;

    public void eval(byte requestByte) {
        parsingState = parsingState.eval(requestByte);
    }
}
