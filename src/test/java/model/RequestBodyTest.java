package model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RequestBodyTest {
    @Test
    void shouldBeFull() {
        RequestBody queue = new RequestBody(() -> {
        });

        for (int c = 1; c <= 8192; c++) {
            queue.enqueue(0);
        }

        Assertions.assertTrue(queue.isFull());
    }

    @Test
    void shouldNotCallOnSpaceFreedCallback() {
        Runnable onSpaceFreed = Mockito.mock(Runnable.class);
        RequestBody queue = new RequestBody(onSpaceFreed);

        for (int c = 1; c <= 8191; c++) {
            queue.enqueue(0);
        }

        queue.dequeue();

        Assertions.assertFalse(queue.isFull());
        Mockito.verify(onSpaceFreed, Mockito.never()).run();
    }

    @Test
    void shouldCallOnSpaceFreedCallback() {
        Runnable onSpaceFreed = Mockito.mock(Runnable.class);
        RequestBody queue = new RequestBody(onSpaceFreed);

        for (int c = 1; c <= 8192; c++) {
            queue.enqueue(0);
        }

        queue.dequeue();

        Assertions.assertFalse(queue.isFull());
        Mockito.verify(onSpaceFreed).run();
    }
}
