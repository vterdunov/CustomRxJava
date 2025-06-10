package com.customrxjava;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Класс представляющий Observable.
 * @param <T> Тип элементов
 */
public class Observable<T> {
    private final Consumer<Observer<T>> source;

    private Observable(Consumer<Observer<T>> source) {
        this.source = source;
    }

    /**
     * Создает новый Observable из функции источника.
     * @param source Функция источник
     * @param <T> Тип элементов
     * @return Новый экземпляр Observable
     */
    public static <T> Observable<T> create(Consumer<Observer<T>> source) {
        return new Observable<>(source);
    }

    /**
     * Подписывает Observer возвращает Disposable.
     * @param observer Observer для подписки
     * @return Disposable для отмены подписки
     */
    public Disposable subscribe(Observer<T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        try {
            source.accept(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (!disposed.get()) {
                        observer.onNext(item);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!disposed.get()) {
                        observer.onError(t);
                    }
                }

                @Override
                public void onComplete() {
                    if (!disposed.get()) {
                        observer.onComplete();
                    }
                }
            });
        } catch (Exception e) {
            if (!disposed.get()) {
                observer.onError(e);
            }
        }
        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    /**
     * Подписывается на Observable с колбэками для onNext onError и onComplete.
     * @param onNext Колбэк для обработки элементов
     * @param onError Колбэк для обработки ошибок
     * @param onComplete Колбэк для обработки завершения
     * @return Disposable для отмены подписки
     */
    public Disposable subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        return subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                onNext.accept(item);
            }

            @Override
            public void onError(Throwable t) {
                onError.accept(t);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }

    /**
     * Преобразует элементы выдаваемые Observable применяя функцию к каждому элементу.
     * @param mapper Функция
     * @param <R> Тип элементов выдаваемых результирующим Observable
     * @return Новый Observable - выдает преобразованные элементы
     */
    public <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    observer.onNext(mapper.apply(item));
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
        ));
    }

    /**
     * Фильтрует элементы выдаваемые этим Observable. выдавая только те которые удовлетворяют предикат.
     * @param predicate Предикат применяемый к каждому элементу
     * @return Новый Observable который выдает только те элементы которые удовлетворяют предикат
     */
    public Observable<T> filter(Predicate<T> predicate) {
        return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    if (predicate.test(item)) {
                        observer.onNext(item);
                    }
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
        ));
    }

    /**
     * Преобразует элементы выдаваемые Observable
     * @param mapper Функция которая возвращает Observable для каждого элемента выдаваемого исходным Observable
     * @param <R> Тип элементов выдаваемых результирующим Observable
     * @return Новый Observable
     */
    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<>(observer -> {
            AtomicBoolean disposed = new AtomicBoolean(false);
            subscribe(
                item -> {
                    if (!disposed.get()) {
                        try {
                            Observable<R> innerObservable = mapper.apply(item);
                            innerObservable.subscribe(
                                observer::onNext,
                                observer::onError,
                                () -> {} // Не завершать когда внутренний завершается
                            );
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                },
                observer::onError,
                observer::onComplete
            );
        });
    }

    /**
     * Указывает Scheduler на котором будет работать Observable.
     * @param scheduler Scheduler
     * @return Новый Observable который работает на установленом Scheduler
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(observer -> scheduler.execute(() -> subscribe(observer)));
    }

    /**
     * Указывает Scheduler на котором Observer будет наблюдать за этим Observable.
     * @param scheduler Scheduler для использования
     * @return Новый Observable который наблюдается на указанном Scheduler
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(observer -> subscribe(
            item -> scheduler.execute(() -> observer.onNext(item)),
            error -> scheduler.execute(() -> observer.onError(error)),
            () -> scheduler.execute(observer::onComplete)
        ));
    }
}
