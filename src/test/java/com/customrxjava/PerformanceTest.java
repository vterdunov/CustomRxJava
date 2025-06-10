package com.customrxjava;

import com.customrxjava.schedulers.ComputationScheduler;
import com.customrxjava.schedulers.IOThreadScheduler;
import com.customrxjava.schedulers.SingleThreadScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTest {

    private ComputationScheduler computationScheduler;
    private IOThreadScheduler ioThreadScheduler;
    private SingleThreadScheduler singleThreadScheduler;

    @BeforeEach
    void setUp() {
        computationScheduler = new ComputationScheduler();
        ioThreadScheduler = new IOThreadScheduler();
        singleThreadScheduler = new SingleThreadScheduler();
    }

    @Test
    void testMapOperatorPerformance() throws InterruptedException {
        int itemCount = 100000;
        CountDownLatch latch = new CountDownLatch(itemCount);
        AtomicInteger processedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.map(i -> i * 2)
                .map(i -> i + 1)
                .map(i -> String.valueOf(i))
                .subscribe(
                    item -> {
                        processedCount.incrementAndGet();
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(itemCount, processedCount.get());
        long duration = endTime - startTime;

        // Проверяем что обработка заняла разумное время (менее 5 секунд)
        assertTrue(duration < 5000,
            String.format("Обработка %d элементов через map заняла %d мс", itemCount, duration));

        // Проверяем производительность - более 10000 элементов в секунду
        long throughput = (itemCount * 1000L) / duration;
        assertTrue(throughput > 10000,
            String.format("Производительность слишком низкая: %d элементов/сек", throughput));
    }

    @Test
    void testFilterOperatorPerformance() throws InterruptedException {
        int itemCount = 50000;
        AtomicInteger filteredCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.filter(i -> i % 2 == 0)
                .filter(i -> i % 10 == 0)
                .subscribe(
                    item -> filteredCount.incrementAndGet(),
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        // Ожидаем примерно 5000 элементов (каждый 10-й из четных)
        int expectedCount = itemCount / 10;
        assertEquals(expectedCount, filteredCount.get());

        long duration = endTime - startTime;
        assertTrue(duration < 3000,
            String.format("Фильтрация %d элементов заняла %d мс", itemCount, duration));
    }

    @Test
    void testFlatMapOperatorPerformance() throws InterruptedException {
        int sourceCount = 1000;
        int innerCount = 10;
        int totalExpected = sourceCount * innerCount;

        CountDownLatch latch = new CountDownLatch(totalExpected);
        AtomicInteger processedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < sourceCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.<String>flatMap(i -> Observable.<String>create(obs -> {
            for (int j = 0; j < innerCount; j++) {
                obs.onNext("item_" + i + "_" + j);
            }
            obs.onComplete();
        })).subscribe(
            item -> {
                processedCount.incrementAndGet();
                latch.countDown();
            },
            error -> {},
            () -> {}
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(totalExpected, processedCount.get());

        long duration = endTime - startTime;
        assertTrue(duration < 8000,
            String.format("FlatMap обработка %d элементов заняла %d мс", totalExpected, duration));
    }

    @Test
    void testHighVolumeDataProcessing() throws InterruptedException {
        int itemCount = 1000000; // 1 миллион элементов
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong sum = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 1; i <= itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.filter(i -> i % 1000 == 0) // Каждый тысячный
                .map(i -> (long) i * i) // Квадрат
                .subscribe(
                    item -> sum.addAndGet(item),
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        // Проверяем корректность результата
        assertTrue(sum.get() > 0);

        long duration = endTime - startTime;
        assertTrue(duration < 10000,
            String.format("Обработка %d элементов заняла %d мс", itemCount, duration));

        System.out.printf("Обработано %d элементов за %d мс (%.2f элементов/мс)%n",
            itemCount, duration, (double) itemCount / duration);
    }

    @Test
    void testConcurrentSchedulerPerformance() throws InterruptedException {
        int taskCount = 10000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < taskCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.subscribeOn(computationScheduler)
                .map(i -> {
                    // Имитируем вычислительную работу
                    double result = Math.sqrt(i * Math.PI);
                    return (int) result;
                })
                .observeOn(ioThreadScheduler)
                .subscribe(
                    item -> {
                        completedTasks.incrementAndGet();
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(taskCount, completedTasks.get());

        long duration = endTime - startTime;
        assertTrue(duration < 10000,
            String.format("Параллельная обработка %d задач заняла %d мс", taskCount, duration));
    }

    @Test
    void testMemoryUsageUnderLoad() throws InterruptedException {
        int batchCount = 100;
        int batchSize = 10000;

        for (int batch = 0; batch < batchCount; batch++) {
            CountDownLatch latch = new CountDownLatch(batchSize);

            Observable<Integer> observable = Observable.create(observer -> {
                for (int i = 0; i < batchSize; i++) {
                    observer.onNext(i);
                }
                observer.onComplete();
            });

            observable.map(i -> "processed_" + i)
                    .filter(s -> s.length() > 10)
                    .subscribe(
                        item -> latch.countDown(),
                        error -> {},
                        () -> {}
                    );

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            // Принудительная сборка мусора между батчами
            if (batch % 10 == 0) {
                System.gc();
                Thread.sleep(50);
            }
        }

        // Если дошли до сюда без OutOfMemoryError - тест пройден
        assertTrue(true, "Обработка множества батчей завершена без ошибок памяти");
    }

    @Test
    void testChainedOperatorsPerformance() throws InterruptedException {
        int itemCount = 50000;
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.filter(i -> i % 2 == 0)
                .map(i -> i * 3)
                .filter(i -> i > 100)
                .map(i -> "result_" + i)
                .filter(s -> s.length() > 8)
                .<String>flatMap(s -> Observable.<String>create(obs -> {
                    obs.onNext(s + "_processed");
                    obs.onComplete();
                }))
                .subscribe(
                    item -> {
                        synchronized (results) {
                            results.add(item);
                        }
                    },
                    error -> {},
                    latch::countDown
                );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertTrue(results.size() > 0);

        long duration = endTime - startTime;
        assertTrue(duration < 8000,
            String.format("Цепочка операторов для %d элементов заняла %d мс", itemCount, duration));

        System.out.printf("Цепочка операторов: %d -> %d элементов за %d мс%n",
            itemCount, results.size(), duration);
    }

        @Test
    void testSchedulerSwitchingPerformance() throws InterruptedException {
        int itemCount = 1000;
        // Подсчитаем ожидаемое количество элементов которые пройдут фильтр
        int expectedCount = 0;
        for (int i = 0; i < itemCount; i++) {
            if (i * 2 > 100) expectedCount++;
        }

        CountDownLatch latch = new CountDownLatch(expectedCount);
        AtomicInteger processedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Observable<Integer> observable = Observable.create(observer -> {
            for (int i = 0; i < itemCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        observable.subscribeOn(singleThreadScheduler)
                .observeOn(computationScheduler)
                .map(i -> i * 2)
                .observeOn(ioThreadScheduler)
                .filter(i -> i > 100)
                .observeOn(singleThreadScheduler)
                .subscribe(
                    item -> {
                        processedCount.incrementAndGet();
                        latch.countDown();
                    },
                    error -> {},
                    () -> {}
                );

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(expectedCount, processedCount.get());

        long duration = endTime - startTime;
        System.out.printf("Переключение планировщиков: %d элементов за %d мс%n", itemCount, duration);

        // Проверяем просто что тест завершился в разумное время
        assertTrue(duration < 15000,
            String.format("Переключение планировщиков для %d элементов заняло %d мс", itemCount, duration));
    }
}
