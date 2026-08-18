package server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import reader.RequestReader;
import writer.ResponseWriter;

import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public class ClientSocketContext {
    private final ResponseWriter responseWriter;

    private final ByteBuffer readBuffer;

    @Setter
    private RequestReader requestReader;
}
