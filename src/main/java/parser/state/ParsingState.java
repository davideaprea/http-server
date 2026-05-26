package parser.state;

public interface ParsingState {
    ParsingState eval(byte requestByte);
}
