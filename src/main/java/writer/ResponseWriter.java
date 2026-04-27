package writer;

import lombok.AllArgsConstructor;
import shared.model.Response;

import java.io.*;
import java.net.Socket;
import java.util.Map;

@AllArgsConstructor
public class ResponseWriter {
    private final Socket clientSocket;

    public void write(Response response) {
        try {
            OutputStream clientOutputStream = clientSocket.getOutputStream();

            clientOutputStream.write("%s %s %s\r\n".formatted(
                    response.version().getValue(),
                    response.status().getCode(),
                    response.status().getName()
            ).getBytes());

            for (Map.Entry<String, String> h : response.headers().entrySet()) {
                clientOutputStream.write("%s: %s\r\n".formatted(h.getKey(), h.getValue()).getBytes());
            }

            clientOutputStream.write("\r\n".getBytes());
            clientOutputStream.flush();

            InputStream responseBodyStream = response.body();

            try (responseBodyStream) {
                byte[] buffer = new byte[8192];
                int bytesNumber;

                while ((bytesNumber = responseBodyStream.read(buffer)) != -1) {
                    clientOutputStream.write(buffer, 0, bytesNumber);
                }

                clientOutputStream.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}
