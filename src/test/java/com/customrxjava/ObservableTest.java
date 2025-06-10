package com.customrxjava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObservableTest {

    private TestObserver<String> observer;

    @BeforeEach
    void setUp() {
        observer = new TestObserver<>();
    }

    @Test
    void testCreateObservable() {
        Observable<String> observable = Observable.create(observer -> {
            observer.onNext("item1");
            observer.onNext("item2");
            observer.onComplete();
        });

        assertNotNull(observable);
    }

    @Test
    void testSubscribe() {
        Observable<String> observable = Observable.create(observer -> {
            observer.onNext("test1");
            observer.onNext("test2");
            observer.onComplete();
        });

        observable.subscribe(observer);

        assertEquals(2, observer.getReceivedItems().size());
        assertEquals("test1", observer.getReceivedItems().get(0));
        assertEquals("test2", observer.getReceivedItems().get(1));
        assertTrue(observer.isCompleted());
        assertNull(observer.getError());
    }

    @Test
    void testSubscribeWithError() {
        RuntimeException error = new RuntimeException("Test error");
        Observable<String> observable = Observable.create(observer -> {
            observer.onNext("item1");
            observer.onError(error);
        });

        observable.subscribe(observer);

        assertEquals(1, observer.getReceivedItems().size());
        assertEquals("item1", observer.getReceivedItems().get(0));
        assertEquals(error, observer.getError());
        assertFalse(observer.isCompleted());
    }

    @Test
    void testMultipleSubscribers() {
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        TestObserver<Integer> observer1 = new TestObserver<>();
        TestObserver<Integer> observer2 = new TestObserver<>();

        observable.subscribe(observer1);
        observable.subscribe(observer2);

        assertEquals(2, observer1.getReceivedItems().size());
        assertEquals(2, observer2.getReceivedItems().size());
        assertTrue(observer1.isCompleted());
        assertTrue(observer2.isCompleted());
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
