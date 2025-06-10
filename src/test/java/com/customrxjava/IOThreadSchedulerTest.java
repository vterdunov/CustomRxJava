package com.customrxjava;

import com.customrxjava.schedulers.IOThreadScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IOThreadSchedulerTest {

    private IOThreadScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new IOThreadScheduler();
    }

    @Test
    void testExecuteTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger();

        scheduler.execute(() -> {
            result.set(100);
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(100, result.get());
    }

    @Test
    void testCachedThreadPoolBehavior() throws InterruptedException {
        int numberOfTasks = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch taskLatch = new CountDownLatch(numberOfTasks);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        // Запускаем много IO задач одновременно
        for (int i = 0; i < numberOfTasks; i++) {
            scheduler.execute(() -> {
                try {
                    startLatch.await();
                    threadNames.add(Thread.currentThread().getName());
                    Thread.sleep(200); // Имитируем IO операцию
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                taskLatch.countDown();
            });
        }

        // Запускаем все задачи одновременно
        startLatch.countDown();
        assertTrue(taskLatch.await(5, TimeUnit.SECONDS));

        // Проверяем что создалось много потоков (характеристика CachedThreadPool)
        assertTrue(threadNames.size() > 1, "Должно быть создано несколько потоков");

        // Для IO операций может создаваться много потоков
        assertTrue(threadNames.size() <= numberOfTasks,
            "Количество потоков не должно превышать количество задач");
    }

    @Test
    void testThreadCreationOnDemand() throws InterruptedException {
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        // Первая задача
        scheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch1.countDown();
        });

        // Ждем немного и запускаем вторую задачу пока первая еще выполняется
        Thread.sleep(50);
        scheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            latch2.countDown();
        });

        assertTrue(latch1.await(2, TimeUnit.SECONDS));
        assertTrue(latch2.await(2, TimeUnit.SECONDS));

        // Проверяем что создался новый поток для второй задачи
        // (поскольку первый поток был занят)
        assertEquals(2, threadNames.size(),
            "Должно быть создано два потока для одновременных задач");
    }

    @Test
    void testThreadReuse() throws InterruptedException {
        Set<String> firstBatchThreads = ConcurrentHashMap.newKeySet();
        Set<String> secondBatchThreads = ConcurrentHashMap.newKeySet();

        CountDownLatch firstBatch = new CountDownLatch(3);

        // Первая партия задач
        for (int i = 0; i < 3; i++) {
            scheduler.execute(() -> {
                firstBatchThreads.add(Thread.currentThread().getName());
                firstBatch.countDown();
            });
        }

        assertTrue(firstBatch.await(2, TimeUnit.SECONDS));

        // Ждем чтобы потоки освободились
        Thread.sleep(100);

        CountDownLatch secondBatch = new CountDownLatch(3);

        // Вторая партия задач
        for (int i = 0; i < 3; i++) {
            scheduler.execute(() -> {
                secondBatchThreads.add(Thread.currentThread().getName());
                secondBatch.countDown();
            });
        }

        assertTrue(secondBatch.await(2, TimeUnit.SECONDS));

        // Проверяем что некоторые потоки переиспользовались
        // (пересечение множеств должно быть не пустым)
        firstBatchThreads.retainAll(secondBatchThreads);
        assertFalse(firstBatchThreads.isEmpty(),
            "Некоторые потоки должны быть переиспользованы");
    }

    @Test
    void testHighConcurrencyIOOperations() throws InterruptedException {
        int taskCount = 50;
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Имитируем много параллельных IO операций
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            scheduler.execute(() -> {
                try {
                    threadNames.add(Thread.currentThread().getName());
                    // Имитируем IO операцию разной длительности
                    Thread.sleep(50 + (taskId % 100));
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(taskCount, completedTasks.get());

        // Проверяем что создалось достаточно потоков для параллельной обработки
        assertTrue(threadNames.size() > 10,
            "Для IO операций должно создаваться много потоков");

        // Проверяем что операции выполнились быстрее чем последовательно
        assertTrue(endTime - startTime < 8000,
            "Параллельные IO операции должны выполняться быстрее");
    }

    @Test
    void testTasksNotInMainThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        String mainThreadName = Thread.currentThread().getName();
        Set<String> taskThreadNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 5; i++) {
            scheduler.execute(() -> {
                taskThreadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Проверяем что ни одна задача не выполнилась в главном потоке
        for (String threadName : taskThreadNames) {
            assertNotEquals(mainThreadName, threadName,
                "Задачи не должны выполняться в главном потоке");
        }
    }

    @Test
    void testTaskException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // Первая задача бросает исключение
        scheduler.execute(() -> {
            latch.countDown();
            throw new RuntimeException("IO Test exception");
        });

        // Вторая задача должна выполниться несмотря на исключение в первой
        scheduler.execute(() -> {
            successCount.incrementAndGet();
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, successCount.get());
    }

    @Test
    void testLongRunningIOOperations() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        // Запускаем долгие IO операции
        for (int i = 0; i < 5; i++) {
            scheduler.execute(() -> {
                try {
                    threadNames.add(Thread.currentThread().getName());
                    Thread.sleep(500); // Длительная IO операция
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        // Проверяем что все операции выполнились параллельно
        assertEquals(5, threadNames.size(),
            "Все долгие IO операции должны выполняться в отдельных потоках");
    }
}
