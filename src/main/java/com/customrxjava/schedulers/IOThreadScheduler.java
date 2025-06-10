package com.customrxjava.schedulers;

import com.customrxjava.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Планировщик для IO операций с кешированным пулом потоков.
 * Аналог Schedulers.io() из RxJava.
 */
public class IOThreadScheduler implements Scheduler {
    private final ExecutorService executor;

    public IOThreadScheduler() {
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
