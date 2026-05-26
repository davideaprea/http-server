package parser;

import parser.state.ParsingState;

public class RequestParser {
    private ParsingState parsingState;

    public void eval(byte requestByte) {
        parsingState = parsingState.eval(requestByte);
    }
}
