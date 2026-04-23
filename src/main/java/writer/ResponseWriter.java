package writer;

import lombok.AllArgsConstructor;
import shared.model.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

@AllArgsConstructor
public class ResponseWriter {
    private final OutputStream outputStream;

    public void write(Response response) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        writer.write(
                response.version().getValue() + " " +
                        response.status().getCode() + " " +
                        response.status().getName() + "\r\n"
        );

        for (Map.Entry<String, String> h : response.headers().entrySet()) {
            writer.write(h.getKey() + ": " + h.getValue() + "\r\n");
        }

        if (!response.headers().containsKey("Content-Length")) {
            writer.write("Content-Length: " + response.body().length + "\r\n");
        }
        writer.write("\r\n");
        writer.flush();

        outputStream.write(response.body());
        outputStream.flush();
    }
}
