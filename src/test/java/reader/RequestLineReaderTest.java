package reader;

import client.channel.ClientRequestsQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RequestLineReaderTest {
    @Test
    void shouldRemainInSameStateWhenReadingRegularCharacters() {
        RequestLineReader reader = new RequestLineReader(Mockito.mock(ClientRequestsQueue.class), () -> {}, Mockito.mock());
        RequestReader result = reader.eval((byte) 'G').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldRemainInSameStateWhenReceivingCarriageReturn() {
        ClientRequestsQueue context = Mockito.mock(ClientRequestsQueue.class);
        RequestLineReader reader = new RequestLineReader(context, () -> {}, Mockito.mock());
        RequestReader result = reader.eval((byte) '\r').nextReader();

        Assertions.assertSame(reader, result);
    }

    @Test
    void shouldPassInReadingHeadersState() {
        ClientRequestsQueue context = Mockito.mock(ClientRequestsQueue.class);
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
