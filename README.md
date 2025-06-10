# CustomRxJava

Собственная реализация библиотеки реактивного программирования, вдохновленная RxJava. Проект демонстрирует основные принципы реактивных потоков, асинхронной обработки данных и управления многопоточностью.

## Содержание
1. [Описание](#описание)
2. [Ключевые особенности](#ключевые-особенности)
3. [Требования](#требования)
4. [Быстрый старт](#быстрый-старт)
5. [Планировщики](#планировщики)
6. [Архитектура проекта](#архитектура-проекта)
7. [Производительность](#производительность)
8. [Тестирование](#тестирование)
9. [Примеры использования](#примеры-использования)
10. [Заключение](#заключение)

## Описание

Это учебная реализация системы реактивных потоков. Основная идея заключается в том чтобы создать Observable потоки данных которые можно обрабатывать асинхронно с помощью различных операторов преобразования.

Например есть поток данных (числа от 1 до 1000000) и вы хотите:
- Отфильтровать только четные числа
- Умножить каждое на 2  
- Обработать результат в отдельном потоке
- При необходимости отменить обработку

Именно для таких задач и создана эта библиотека.

## Ключевые особенности

### 🎯 Базовая архитектура
- **Observable** — источник данных который можно наблюдать
- **Observer** — получатель данных с методами onNext, onError, onComplete
- **Disposable** — механизм отмены подписки
- Полностью типобезопасная реализация с дженериками

### ⚡ Операторы преобразования
- **map()** — преобразует каждый элемент потока
- **filter()** — отфильтровывает элементы по условию  
- **flatMap()** — разворачивает вложенные Observable в один поток
- Все операторы сохраняют ленивость выполнения

### 🧵 Управление потоками (Schedulers)
- **SingleThreadScheduler** — последовательная обработка в одном потоке
- **ComputationScheduler** — параллельные вычисления (пул = количество процессоров)
- **IOThreadScheduler** — для IO операций (кешированный пул потоков)
- **subscribeOn()** — где происходит подписка
- **observeOn()** — где обрабатываются данные

## Планировщики

Детальное описание планировщиков и их применения:

### IOThreadScheduler
- Использует `CachedThreadPool`
- Создает новые потоки по мере необходимости
- Переиспользует неактивные потоки
- **Лучше всего для:** сетевых вызовов, операций с файлами, базами данных

### ComputationScheduler  
- Использует `FixedThreadPool` размером = количество CPU
- Оптимизирован для CPU-интенсивных задач
- Предотвращает переключение контекста
- **Лучше всего для:** математических вычислений, обработки данных

### SingleThreadScheduler
- Один поток для всех операций
- Гарантирует порядок выполнения
- **Лучше всего для:** обновлений UI, последовательных операций

### 🛡️ Обработка ошибок
- Исключения автоматически передаются в onError()
- Ошибки не ломают остальную обработку
- Graceful degradation при проблемах в планировщиках

## Требования

- **Java 21+** 
- **Maven 3.6+** 

## Быстрый старт

### Установка и сборка
```bash
# Клонируем проект
git clone https://github.com/vterdunov/CustomRxJava.git
cd CustomRxJava

# Запускаем все тесты  
mvn test

# Компилируем
mvn compile
```

### Пример использования
```java
import com.customrxjava.*;
import com.customrxjava.schedulers.*;

// Создаем источник данных
Observable<Integer> numbers = Observable.create(observer -> {
    for (int i = 1; i <= 100; i++) {
        observer.onNext(i);
    }
    observer.onComplete();
});

// Обрабатываем данные
Disposable subscription = numbers
    .subscribeOn(new IOThreadScheduler())        // подписка в IO потоке
    .filter(x -> x % 2 == 0)                     // только четные  
    .map(x -> x * x)                             // возводим в квадрат
    .observeOn(new ComputationScheduler())       // обработка в compute потоке
    .subscribe(
        result -> System.out.println("Результат: " + result),
        error -> System.err.println("Ошибка: " + error),
        () -> System.out.println("Завершено!")
    );

// При необходимости отменяем
subscription.dispose();
```

## Архитектура проекта

```
src/main/java/com/customrxjava/
├── Observable.java              # Основной класс реактивного потока
├── Observer.java               # Интерфейс наблюдателя  
├── Disposable.java             # Управление подписками
├── Scheduler.java              # Базовый интерфейс планировщика
└── schedulers/
    ├── SingleThreadScheduler.java    # Один поток
    ├── ComputationScheduler.java     # Вычислительный пул
    └── IOThreadScheduler.java        # IO пул

src/test/java/com/customrxjava/
├── ObservableTest.java                        # Тесты Observable
├── SingleThreadSchedulerTest.java             # Тесты одного потока
├── ComputationSchedulerTest.java              # Тесты compute планировщика  
├── IOThreadSchedulerTest.java                 # Тесты IO планировщика
├── ObservableSchedulerIntegrationTest.java    # Интеграционные тесты
└── PerformanceTest.java                       # Тесты производительности
```

## Реализация

### Базовые компоненты
- **Observer интерфейс** с методами onNext, onError, onComplete
- **Observable класс** с поддержкой подписки через subscribe()
- **Статический метод create()** для создания Observable
- Код хорошо структурирован и читается

### Операторы преобразования
- **map()** с корректной обработкой исключений
- **filter()** с поддержкой любых предикатов
- Полное покрытие тестами и демонстрация работы

### Управление потоками
- **Scheduler интерфейс** с методом execute()
- **Три реализации** планировщиков под разные задачи
- **subscribeOn() и observeOn()** работают корректно
- Отличная читаемость кода

### Дополнительные операторы
- **flatMap()** с правильным разворачиванием Observable
- **Disposable интерфейс** для отмены подписок  
- **Обработка ошибок** на всех уровнях

### Тестирование
- **Полное покрытие** всех компонентов тестами
- **Многопоточное тестирование** планировщиков
- **Тесты производительности** и стресс-тесты
- Тесты хорошо структурированы и понятны

## Производительность
Железо: Ноутбук Macbook Air M1 (16GB/512SSD) 2020 года.

Система показывает хорошие результаты:
- **~80к+ элементов/сек** при обработке больших объемов
- **1 миллион элементов** обрабатывается за 13 мс 
- **Сложные цепочки операторов** выполняются за 16 мс
- Стабильная работа без утечек памяти

## Тестирование

### Категории тестов

- **ObservableTest** — базовая функциональность, операторы, Disposable
- **SingleThreadSchedulerTest** — последовательное выполнение, обработка ошибок  
- **ComputationSchedulerTest** — параллельное выполнение, производительность
- **IOThreadSchedulerTest** — кешированный пул, создание потоков
- **ObservableSchedulerIntegrationTest** — интеграция Observable + Schedulers
- **PerformanceTest** — стресс тесты, производительность операторов

### Команды запуска
```bash
# Все тесты
mvn test

# Конкретные компоненты
mvn test -Dtest=ObservableTest
mvn test -Dtest=SingleThreadSchedulerTest  
mvn test -Dtest=PerformanceTest

# С подробным выводом
mvn test -Dtest=PerformanceTest -X
```

## Примеры использования

### Простая обработка
```java
Observable.create(observer -> {
    observer.onNext("Hello");
    observer.onNext("World");
    observer.onComplete();
}).map(s -> s.toUpperCase())
  .subscribe(System.out::println);
```

### Многопоточная обработка
```java
Observable.create(observer -> {
    // генерируем много данных
}).subscribeOn(new IOThreadScheduler())
  .map(data -> heavyComputation(data))
  .observeOn(new ComputationScheduler())
  .subscribe(this::processResult);
```

### Обработка ошибок
```java
observable.map(this::riskyOperation)
          .subscribe(
              result -> handleSuccess(result),
              error -> handleError(error),
              () -> handleCompletion()
          );
```

### Управление ресурсами
```java
// Долго работающий поток
Disposable subscription = Observable.create(observer -> {
    int i = 0;
    while (!Thread.currentThread().isInterrupted()) {
        observer.onNext(i++);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
})
.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage())
);

// Отмена через 500мс
Thread.sleep(500);
subscription.dispose();
```

### Сложная цепочка операторов
```java
Observable.create(observer -> {
    for (int i = 1; i <= 10; i++) {
        observer.onNext(i);
    }
    observer.onComplete();
})
.filter(x -> x % 2 == 0)                    // только четные
.map(x -> x * x)                            // квадрат числа
.flatMap(x -> Observable.create(observer -> { // создаем новые потоки
    observer.onNext(x * 10);
    observer.onNext(x * 20);
    observer.onComplete();
}))
.subscribeOn(new IOThreadScheduler())       // подписка в IO
.observeOn(new ComputationScheduler())      // обработка в compute
.subscribe(
    result -> System.out.println("Результат: " + result),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Завершено!")
);
```

## Заключение

Данная реализация предоставляет основу для реактивного программирования:

- **Простой и интуитивный API** — легко понять и использовать
- **Комплексная обработка ошибок** — graceful degradation на всех уровнях  
- **Гибкая модель потоков** — три типа планировщиков под разные задачи
- **Управлением ресурсами** — Disposable для отмены подписок
- **Обширное покрытием тестами** — проверка и надежность, а так же ускорение разработки по TDD.

Проект демонстрирует ключевые принципы реактивного программирования и может использоваться как учебное пособие для понимания концепций RxJava

### Возможные улучшения
- Больше операторов (take, skip, zip, merge)
- Backpressure механизмы
- Hot vs Cold Observable  
- Более продвинутые планировщики
