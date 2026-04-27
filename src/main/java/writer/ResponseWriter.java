package writer;

import lombok.AllArgsConstructor;
import shared.model.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

@AllArgsConstructor
public class ResponseWriter {
    private final Socket clientSocket;

    public void write(Response response) {
        try {
            OutputStream clientOutputStream = clientSocket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientOutputStream));

            writer.write("%s %s %s\r\n".formatted(
                    response.version().getValue(),
                    response.status().getCode(),
                    response.status().getName()
            ));

            for (Map.Entry<String, String> h : response.headers().entrySet()) {
                writer.write("%s: %s\r\n".formatted(h.getKey(), h.getValue()));
            }

            writer.write("\r\n");
            writer.flush();

            clientOutputStream.write(response.body());
            clientOutputStream.flush();
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}
