package reader.state;

import lombok.AllArgsConstructor;
import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

import java.util.Optional;

@AllArgsConstructor
public class BodyReadingModeSelector {
    private final RequestContext context;

    public ParsingState evalFromRequest(Request request) {
        Optional<Long> contentLengthValue = request.getContentLength();
        Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

        if (contentLengthValue.isEmpty() && transferEncodingValue.isEmpty()) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        if (contentLengthValue.isPresent()) {
            return new ContentLengthBodyState(new ContentLengthRequest(
                    request.body(),
                    contentLengthValue.get()
            ), context);
        }

        return new ChunkedBodyState(request.body(), context);
    }
}
