package writer;

import lombok.AllArgsConstructor;
import shared.model.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Map;

@AllArgsConstructor
public class ResponseWriter {
    private final OutputStream outputStream;

    public void write(Response response) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        response.headers().put("Content-Length", String.valueOf(response.body().length));

        writer.write(
                response.version().getValue() + " " +
                        response.status().getCode() + " " +
                        response.status().getName() + "\r\n"
        );

        for (Map.Entry<String, String> h : response.headers().entrySet()) {
            writer.write(h.getKey() + ": " + h.getValue() + "\r\n");
        }

        writer.write("\r\n");
        writer.write(Arrays.toString(response.body()));
        writer.flush();
    }
}
