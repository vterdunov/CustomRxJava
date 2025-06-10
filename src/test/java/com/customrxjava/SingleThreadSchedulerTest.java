package com.customrxjava;

import com.customrxjava.schedulers.SingleThreadScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SingleThreadSchedulerTest {

    private SingleThreadScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SingleThreadScheduler();
    }

    @Test
    void testExecuteTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        scheduler.execute(() -> {
            result.set("task executed");
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("task executed", result.get());
    }

    @Test
    void testSequentialExecution() throws InterruptedException {
        List<Integer> executionOrder = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        // Запускаем три задачи
        scheduler.execute(() -> {
            try {
                Thread.sleep(50); // Имитируем работу
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executionOrder.add(1);
            latch.countDown();
        });

        scheduler.execute(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executionOrder.add(2);
            latch.countDown();
        });

        scheduler.execute(() -> {
            executionOrder.add(3);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Проверяем что задачи выполнились в порядке подачи
        assertEquals(3, executionOrder.size());
        assertEquals(Integer.valueOf(1), executionOrder.get(0));
        assertEquals(Integer.valueOf(2), executionOrder.get(1));
        assertEquals(Integer.valueOf(3), executionOrder.get(2));
    }

    @Test
    void testSingleThreadExecution() throws InterruptedException {
        List<String> threadNames = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(5);

        // Запускаем несколько задач
        for (int i = 0; i < 5; i++) {
            scheduler.execute(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Проверяем что все задачи выполнились в одном и том же потоке
        assertEquals(5, threadNames.size());
        String firstThreadName = threadNames.get(0);
        for (String threadName : threadNames) {
            assertEquals(firstThreadName, threadName);
        }

        // Проверяем что это не главный поток
        assertNotEquals(Thread.currentThread().getName(), firstThreadName);
    }

    @Test
    void testMultipleTasksInSameThread() throws InterruptedException {
        AtomicReference<String> sharedThreadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.execute(() -> {
            sharedThreadName.set(Thread.currentThread().getName());
        });

        // Даем время первой задаче выполниться
        Thread.sleep(100);

        scheduler.execute(() -> {
            // Проверяем что мы в том же потоке
            assertEquals(sharedThreadName.get(), Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testTaskException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<String> result = new AtomicReference<>();

        // Первая задача бросает исключение
        scheduler.execute(() -> {
            latch.countDown();
            throw new RuntimeException("Test exception");
        });

        // Вторая задача должна выполниться несмотря на исключение в первой
        scheduler.execute(() -> {
            result.set("second task executed");
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("second task executed", result.get());
    }
}
