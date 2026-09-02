package common.queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RequestBodyBytesQueueTest {
    @Test
    void a() {
        RequestBodyBytesQueue queue = new RequestBodyBytesQueue(() -> {
        });

        for (int c = 1; c <= 8192; c++) {
            queue.enqueue(0);
        }

        Assertions.assertTrue(queue.isFull());
    }

    @Test
    void b() {
        Runnable onSpaceFreed = Mockito.mock(Runnable.class);
        RequestBodyBytesQueue queue = new RequestBodyBytesQueue(onSpaceFreed);

        for (int c = 1; c <= 8191; c++) {
            queue.enqueue(0);
        }

        queue.dequeue();

        Assertions.assertFalse(queue.isFull());
        Mockito.verify(onSpaceFreed, Mockito.never()).run();
    }

    @Test
    void c() {
        Runnable onSpaceFreed = Mockito.mock(Runnable.class);
        RequestBodyBytesQueue queue = new RequestBodyBytesQueue(onSpaceFreed);

        for (int c = 1; c <= 8192; c++) {
            queue.enqueue(0);
        }

        queue.dequeue();

        Assertions.assertFalse(queue.isFull());
        Mockito.verify(onSpaceFreed).run();
    }
}
