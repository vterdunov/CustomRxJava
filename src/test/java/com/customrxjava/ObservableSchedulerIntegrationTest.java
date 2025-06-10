package com.customrxjava;

import com.customrxjava.schedulers.ComputationScheduler;
import com.customrxjava.schedulers.IOThreadScheduler;
import com.customrxjava.schedulers.SingleThreadScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ObservableSchedulerIntegrationTest {

    private SingleThreadScheduler singleThreadScheduler;
    private ComputationScheduler computationScheduler;
    private IOThreadScheduler ioThreadScheduler;

    @BeforeEach
    void setUp() {
        singleThreadScheduler = new SingleThreadScheduler();
        computationScheduler = new ComputationScheduler();
        ioThreadScheduler = new IOThreadScheduler();
    }

    @Test
    void testSubscribeOnSingleThread() throws InterruptedException {
        String mainThreadName = Thread.currentThread().getName();
        AtomicReference<String> subscriptionThreadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> observable = Observable.create(observer -> {
            subscriptionThreadName.set(Thread.currentThread().getName());
            observer.onNext("test");
            observer.onComplete();
        });

        observable.subscribeOn(singleThreadScheduler)
                .subscribe(
                    item -> {},
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(subscriptionThreadName.get());
        assertNotEquals(mainThreadName, subscriptionThreadName.get());
    }

    @Test
    void testObserveOnIOScheduler() throws InterruptedException {
        String mainThreadName = Thread.currentThread().getName();
        AtomicReference<String> observerThreadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> observable = Observable.create(observer -> {
            observer.onNext("test");
            observer.onComplete();
        });

        observable.observeOn(ioThreadScheduler)
                .subscribe(
                    item -> observerThreadName.set(Thread.currentThread().getName()),
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(observerThreadName.get());
        assertNotEquals(mainThreadName, observerThreadName.get());
    }

    @Test
    void testSubscribeOnAndObserveOnChain() throws InterruptedException {
        String mainThreadName = Thread.currentThread().getName();
        AtomicReference<String> subscriptionThreadName = new AtomicReference<>();
        AtomicReference<String> observerThreadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> observable = Observable.create(observer -> {
            subscriptionThreadName.set(Thread.currentThread().getName());
            observer.onNext("test");
            observer.onComplete();
        });

        observable.subscribeOn(singleThreadScheduler)
                .observeOn(ioThreadScheduler)
                .subscribe(
                    item -> observerThreadName.set(Thread.currentThread().getName()),
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Проверяем что подписка произошла в SingleThread
        assertNotNull(subscriptionThreadName.get());
        assertNotEquals(mainThreadName, subscriptionThreadName.get());

        // Проверяем что наблюдение произошло в IO потоке
        assertNotNull(observerThreadName.get());
        assertNotEquals(mainThreadName, observerThreadName.get());

        // Проверяем что это разные потоки
        assertNotEquals(subscriptionThreadName.get(), observerThreadName.get());
    }

    @Test
    void testMapOperatorWithSchedulers() throws InterruptedException {
        List<String> results = new ArrayList<>();
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(3);

        Observable<Integer> observable = Observable.create(observer -> {
            threadNames.add(Thread.currentThread().getName());
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        observable.subscribeOn(computationScheduler)
                .map(i -> {
                    threadNames.add(Thread.currentThread().getName());
                    return "value_" + i;
                })
                .observeOn(ioThreadScheduler)
                .subscribe(
                    item -> {
                        threadNames.add(Thread.currentThread().getName());
                        results.add(item);
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(3, results.size());
        assertEquals("value_1", results.get(0));
        assertEquals("value_2", results.get(1));
        assertEquals("value_3", results.get(2));

        // Проверяем что использовались разные потоки
        assertTrue(threadNames.size() > 1);
    }

    @Test
    void testFilterOperatorWithSchedulers() throws InterruptedException {
        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onNext(4);
            observer.onComplete();
        });

        observable.subscribeOn(singleThreadScheduler)
                .filter(i -> i % 2 == 0)
                .observeOn(computationScheduler)
                .subscribe(
                    item -> {
                        results.add(item);
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(2, results.size());
        assertEquals(Integer.valueOf(2), results.get(0));
        assertEquals(Integer.valueOf(4), results.get(1));
    }

    @Test
    void testFlatMapOperatorWithSchedulers() throws InterruptedException {
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(4);

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        observable.subscribeOn(ioThreadScheduler)
                .<String>flatMap(i -> Observable.<String>create(obs -> {
                    obs.onNext("item_" + i + "_a");
                    obs.onNext("item_" + i + "_b");
                    obs.onComplete();
                }))
                .observeOn(computationScheduler)
                .subscribe(
                    item -> {
                        results.add(item);
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        assertEquals(4, results.size());
        assertTrue(results.contains("item_1_a"));
        assertTrue(results.contains("item_1_b"));
        assertTrue(results.contains("item_2_a"));
        assertTrue(results.contains("item_2_b"));
    }

        @Test
    void testMultithreadedProcessing() throws InterruptedException {
        int itemCount = 10;
        List<Integer> filteredItems = new ArrayList<>();
        // Сначала определим сколько элементов пройдет фильтр
        for (int i = 1; i <= itemCount; i++) {
            int doubled = i * 2;
            if (doubled > 10) {
                filteredItems.add(doubled);
            }
        }

        CountDownLatch latch = new CountDownLatch(filteredItems.size());
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        List<Integer> results = new ArrayList<>();

        Observable<Integer> observable = Observable.create(observer -> {
            threadNames.add("source:" + Thread.currentThread().getName());
            for (int i = 1; i <= itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.subscribeOn(ioThreadScheduler)
                .map(i -> {
                    threadNames.add("map:" + Thread.currentThread().getName());
                    // Имитируем вычислительную работу
                    return i * 2;
                })
                .filter(i -> {
                    threadNames.add("filter:" + Thread.currentThread().getName());
                    return i > 10;
                })
                .observeOn(computationScheduler)
                .subscribe(
                    item -> {
                        threadNames.add("observe:" + Thread.currentThread().getName());
                        synchronized (results) {
                            results.add(item);
                        }
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Проверяем что результаты корректны
        assertEquals(filteredItems.size(), results.size());
        for (Integer result : results) {
            assertTrue(result > 10);
            assertTrue(result % 2 == 0);
        }

        // Проверяем что использовались разные потоки на разных этапах
        assertTrue(threadNames.size() >= 2,
            "Должны использоваться разные потоки: " + threadNames);
    }

    @Test
    void testErrorHandlingWithSchedulers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        String mainThreadName = Thread.currentThread().getName();
        AtomicReference<String> errorThreadName = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onError(new RuntimeException("Test error"));
        });

        observable.subscribeOn(singleThreadScheduler)
                .observeOn(ioThreadScheduler)
                .subscribe(
                    item -> {},
                    throwable -> {
                        errorThreadName.set(Thread.currentThread().getName());
                        error.set(throwable);
                        latch.countDown();
                    },
                    () -> {}
                );

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertNotNull(error.get());
        assertEquals("Test error", error.get().getMessage());
        assertNotNull(errorThreadName.get());
        assertNotEquals(mainThreadName, errorThreadName.get());
    }

    @Test
    void testDisposableWithSchedulers() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        Observable<String> observable = Observable.create(observer -> {
            try {
                startLatch.countDown();
                Thread.sleep(200); // Имитируем долгую операцию
                observer.onNext("should not receive");
                observer.onComplete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Disposable disposable = observable.subscribeOn(ioThreadScheduler)
                .observeOn(computationScheduler)
                .subscribe(
                    item -> {
                        result.set(item);
                        endLatch.countDown();
                    },
                    error -> endLatch.countDown(),
                    endLatch::countDown
                );

        // Ждем начала выполнения и сразу отменяем
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        disposable.dispose();

        // Проверяем что подписка отменена
        assertTrue(disposable.isDisposed());

        // Ждем немного чтобы убедиться что операция прервалась
        assertFalse(endLatch.await(500, TimeUnit.MILLISECONDS));
        assertNull(result.get());
    }
}
