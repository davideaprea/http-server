package server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import reader.RequestReader;
import client.ClientOutputChannel;

import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public class ClientSocketContext {
    private final ClientOutputChannel clientOutputChannel;

    private final ByteBuffer readBuffer;

    @Setter
    private RequestReader requestReader;
}
