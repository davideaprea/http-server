package writer;

import model.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Map;

public class ResponseWriter {
    private final OutputStream outputStream;

    public ResponseWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(Response response) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        byte[] bodyBytes = response.body() != null ? response.body().toString().getBytes() : new byte[0];

        response.headers().put("Content-Length", String.valueOf(bodyBytes.length));

        writer.write(
                response.version().getValue() + " " +
                        response.status().getCode() + " " +
                        response.status().getName() + "\r\n"
        );

        for (Map.Entry<String, String> h : response.headers().entrySet()) {
            writer.write(h.getKey() + ": " + h.getValue() + "\r\n");
        }

        writer.write("\r\n");
        writer.write(Arrays.toString(bodyBytes));
        writer.flush();
    }
}
