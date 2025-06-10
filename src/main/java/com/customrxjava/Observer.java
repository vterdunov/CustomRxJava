package com.customrxjava;

/**
 * Интерфейс Observer для реактивных потоков
 */
public interface Observer<T> {

    /**
     * Получает элементы потока
     * @param item элемент потока
     */
    void onNext(T item);

    /**
     * Обрабатывает ошибки
     * @param t исключение
     */
    void onError(Throwable t);

    /**
     * Вызывается при завершении потока
     */
    void onComplete();
}
