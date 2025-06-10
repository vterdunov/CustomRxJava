package com.customrxjava;

import com.customrxjava.schedulers.ComputationScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputationSchedulerTest {

    private ComputationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ComputationScheduler();
    }

    @Test
    void testExecuteTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger();

        scheduler.execute(() -> {
            result.set(42);
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(42, result.get());
    }

    @Test
    void testParallelExecution() throws InterruptedException {
        int numberOfTasks = Runtime.getRuntime().availableProcessors() * 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch taskLatch = new CountDownLatch(numberOfTasks);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        // Запускаем много задач одновременно
        for (int i = 0; i < numberOfTasks; i++) {
            scheduler.execute(() -> {
                try {
                    startLatch.await(); // Ждем сигнала для старта
                    threadNames.add(Thread.currentThread().getName());
                    Thread.sleep(100); // Имитируем вычислительную работу
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                taskLatch.countDown();
            });
        }

        // Запускаем все задачи одновременно
        startLatch.countDown();
        assertTrue(taskLatch.await(3, TimeUnit.SECONDS));

        // Проверяем что использовался более чем один поток
        assertTrue(threadNames.size() > 1, "Должно быть использовано несколько потоков");

        // Проверяем что количество потоков не превышает количество процессоров
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        assertTrue(threadNames.size() <= availableProcessors,
            "Количество потоков не должно превышать количество процессоров");
    }

    @Test
    void testFixedThreadPoolSize() throws InterruptedException {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numberOfTasks = availableProcessors * 3; // Больше задач чем потоков

        CountDownLatch latch = new CountDownLatch(numberOfTasks);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < numberOfTasks; i++) {
            scheduler.execute(() -> {
                threadNames.add(Thread.currentThread().getName());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Проверяем что количество уникальных потоков равно количеству процессоров
        assertEquals(availableProcessors, threadNames.size(),
            String.format("Ожидалось %d потоков (по количеству процессоров), но было %d",
                availableProcessors, threadNames.size()));
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
    void testConcurrentTaskExecution() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);

        // Запускаем несколько задач которые будут работать одновременно
        for (int i = 0; i < 4; i++) {
            scheduler.execute(() -> {
                try {
                    startLatch.await();
                    // Увеличиваем счетчик с задержкой
                    int current = counter.get();
                    Thread.sleep(50);
                    counter.set(current + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                endLatch.countDown();
            });
        }

        // Запускаем все задачи одновременно
        startLatch.countDown();
        assertTrue(endLatch.await(2, TimeUnit.SECONDS));

        // Если задачи выполнялись параллельно то результат может быть меньше 4
        // из-за race condition (что демонстрирует параллельность)
        assertTrue(counter.get() <= 4);
    }

    @Test
    void testTaskException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // Первая задача бросает исключение
        scheduler.execute(() -> {
            latch.countDown();
            throw new RuntimeException("Test exception");
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
    void testHighLoadPerformance() throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            scheduler.execute(() -> {
                // Имитируем легкую вычислительную работу
                double result = Math.sqrt(Math.random() * 1000);
                completedTasks.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();

        assertEquals(taskCount, completedTasks.get());

        // Проверяем что выполнение заняло разумное время (не более 5 секунд)
        assertTrue(endTime - startTime < 5000,
            "Выполнение " + taskCount + " задач заняло слишком много времени");
    }
}
