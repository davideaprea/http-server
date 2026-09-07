package reader;

import client.ClientRequestsQueue;
import model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reader.lifecycle.HeadersReader;
import reader.lifecycle.RequestLineReader;
import reader.lifecycle.RequestReader;

import java.util.function.Consumer;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(ClientRequestsQueue.class), () -> {}, Mockito.mock());
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        Consumer<Request> context = Mockito.mock(ClientRequestsQueue.class);
        RequestLineReader reader = new RequestLineReader(context, () -> {}, Mockito.mock());
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        Consumer<Request> context = Mockito.mock(ClientRequestsQueue.class);
        String rawRequest = "GET /path HTTP/1.1";
        RequestReader state = new RequestLineReader(context, () -> {}, Mockito.mock());

        for(int i = 0; i < rawRequest.length(); i++) {
            char c = rawRequest.charAt(i);
            state = state.eval((byte) c).nextReader();
        }

        state = state.eval((byte) '\r').nextReader();
        state = state.eval((byte) '\n').nextReader();

        Assertions.assertInstanceOf(HeadersReader.class, state);
    }
}
