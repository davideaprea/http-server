package server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import reader.RequestReader;
import writer.ResponseWriter;

@Getter
@AllArgsConstructor
public class ClientSocketContext {
    private final ResponseWriter responseWriter;

    @Setter
    private RequestReader requestReader;
}
