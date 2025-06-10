package com.customrxjava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ObserverTest {

    private TestObserver<String> observer;

    @BeforeEach
    void setUp() {
        observer = new TestObserver<>();
    }

    @Test
    void testOnNext() {
        observer.onNext("test1");
        observer.onNext("test2");

        assertEquals(2, observer.getReceivedItems().size());
        assertEquals("test1", observer.getReceivedItems().get(0));
        assertEquals("test2", observer.getReceivedItems().get(1));
    }

    @Test
    void testOnError() {
        RuntimeException error = new RuntimeException("Test error");
        observer.onError(error);

        assertEquals(error, observer.getError());
    }

    @Test
    void testOnComplete() {
        observer.onComplete();

        assertTrue(observer.isCompleted());
    }

    @Test
    void testCompleteSequence() {
        observer.onNext("item1");
        observer.onNext("item2");
        observer.onComplete();

        assertEquals(2, observer.getReceivedItems().size());
        assertTrue(observer.isCompleted());
        assertNull(observer.getError());
    }

    @Test
    void testErrorSequence() {
        observer.onNext("item1");
        RuntimeException error = new RuntimeException("Error occurred");
        observer.onError(error);

        assertEquals(1, observer.getReceivedItems().size());
        assertEquals(error, observer.getError());
        assertFalse(observer.isCompleted());
    }

    // Тестовая реализация Observer
    private static class TestObserver<T> implements Observer<T> {
        private final List<T> receivedItems = new ArrayList<>();
        private Throwable error;
        private boolean completed = false;

        @Override
        public void onNext(T item) {
            receivedItems.add(item);
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onComplete() {
            this.completed = true;
        }

        public List<T> getReceivedItems() {
            return receivedItems;
        }

        public Throwable getError() {
            return error;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
