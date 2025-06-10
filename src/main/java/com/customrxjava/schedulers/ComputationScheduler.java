package com.customrxjava.schedulers;

import com.customrxjava.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Планировщик для вычислительных операций с фиксированным пулом потоков.
 * Аналог Schedulers.computation() из RxJava.
 */
public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor;

    public ComputationScheduler() {
        // Размер пула равен количеству доступных процессоров
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
