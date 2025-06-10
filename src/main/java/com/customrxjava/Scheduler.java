package com.customrxjava;

/**
 * Интерфейс для планирования задач.
 */
public interface Scheduler {
    /**
     * Планирует задачу для выполнения.
     * @param task Задача для выполнения
     */
    void execute(Runnable task);
}
