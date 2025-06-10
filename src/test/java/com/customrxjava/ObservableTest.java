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

    @Test
    void testMapOperator() {
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        TestObserver<String> stringObserver = new TestObserver<>();
        observable.map(i -> "value_" + i).subscribe(stringObserver);

        assertEquals(3, stringObserver.getReceivedItems().size());
        assertEquals("value_1", stringObserver.getReceivedItems().get(0));
        assertEquals("value_2", stringObserver.getReceivedItems().get(1));
        assertEquals("value_3", stringObserver.getReceivedItems().get(2));
        assertTrue(stringObserver.isCompleted());
    }

    @Test
    void testFilterOperator() {
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onNext(4);
            observer.onComplete();
        });

        TestObserver<Integer> filteredObserver = new TestObserver<>();
        observable.filter(i -> i % 2 == 0).subscribe(filteredObserver);

        assertEquals(2, filteredObserver.getReceivedItems().size());
        assertEquals(Integer.valueOf(2), filteredObserver.getReceivedItems().get(0));
        assertEquals(Integer.valueOf(4), filteredObserver.getReceivedItems().get(1));
        assertTrue(filteredObserver.isCompleted());
    }

    @Test
    void testFlatMapOperator() {
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        TestObserver<String> flatMappedObserver = new TestObserver<>();
        observable.<String>flatMap(i -> Observable.<String>create(obs -> {
            obs.onNext("item_" + i + "_a");
            obs.onNext("item_" + i + "_b");
            obs.onComplete();
        })).subscribe(flatMappedObserver);

        assertEquals(4, flatMappedObserver.getReceivedItems().size());
        assertTrue(flatMappedObserver.getReceivedItems().contains("item_1_a"));
        assertTrue(flatMappedObserver.getReceivedItems().contains("item_1_b"));
        assertTrue(flatMappedObserver.getReceivedItems().contains("item_2_a"));
        assertTrue(flatMappedObserver.getReceivedItems().contains("item_2_b"));
    }

    @Test
    void testDisposable() {
        AtomicBoolean disposed = new AtomicBoolean(false);
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            if (!disposed.get()) {
                observer.onNext(2);
            }
            observer.onComplete();
        });

        TestObserver<Integer> testObserver = new TestObserver<>();
        Disposable disposable = observable.subscribe(testObserver);

        assertFalse(disposable.isDisposed());
        disposable.dispose();
        assertTrue(disposable.isDisposed());
    }

    @Test
    void testSubscribeWithCallbacks() {
        Observable<String> observable = Observable.create(observer -> {
            observer.onNext("test1");
            observer.onNext("test2");
            observer.onComplete();
        });

        List<String> items = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicBoolean hasError = new AtomicBoolean(false);

        observable.subscribe(
            items::add,
            error -> hasError.set(true),
            () -> completed.set(true)
        );

        assertEquals(2, items.size());
        assertEquals("test1", items.get(0));
        assertEquals("test2", items.get(1));
        assertTrue(completed.get());
        assertFalse(hasError.get());
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
