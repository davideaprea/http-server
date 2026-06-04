package reader.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reader.ChunkedBodyReader;
import reader.ContentLengthBodyReader;
import reader.ReadingState;
import reader.dto.ContentLengthRequest;
import reader.dto.RequestContext;
import shared.exception.ResponseStatusException;
import shared.model.Request;
import shared.model.Status;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BodyReadingModeSelector {
    public static ReadingState evalFromRequest(Request request, RequestContext context) {
        Optional<Long> contentLengthValue = request.getContentLength();
        Optional<String> transferEncodingValue = request.getTransferEncoding().filter("chunked"::equals);

        if (contentLengthValue.isEmpty() && transferEncodingValue.isEmpty()) {
            throw new ResponseStatusException("", Status.BAD_REQUEST);
        }

        if (contentLengthValue.isPresent()) {
            return new ContentLengthBodyReader(new ContentLengthRequest(
                    request.body(),
                    contentLengthValue.get()
            ), context);
        }

        return new ChunkedBodyReader(request.body(), context);
    }
}
