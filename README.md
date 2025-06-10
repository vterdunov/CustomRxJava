# CustomRxJava

Собственная реализация библиотеки RxJava с использованием основных концепций реактивного программирования.

## Требования

- Java 21+
- Maven 3.6+

## Структура проекта

```
CustomRxJava/
├── src/
│   ├── main/java/com/customrxjava/     # Основной код
│   └── test/java/com/customrxjava/     # Тесты
├── pom.xml                             # Maven конфигурация
└── README.md                           # Описание проекта
```

## Запуск проекта

### Компиляция проекта
```bash
mvn compile
```

### Запуск всех тестов
```bash
mvn test
```

### Запуск конкретного теста
```bash
mvn test -Dtest=ObserverTest
mvn test -Dtest=ObservableTest
```

### Очистка и пересборка
```bash
mvn clean compile
```

### Полная сборка с тестами
```bash
mvn clean test
```

## Реализованные компоненты

### ✅ Базовые компоненты
- [x] `Observer<T>` - интерфейс для получения событий потока

### 🚧 В разработке
- [ ] `Observable<T>` - класс реактивного потока
- [ ] Операторы `map()`, `filter()`
- [ ] Schedulers (IO, Computation, Single)
- [ ] Оператор `flatMap()`
- [ ] `Disposable` интерфейс

## Разработка

Проект использует TDD подход:
1. Сначала пишется тест
2. Запускается "красный" тест
3. Пишется реализация
4. Повторный запуск теста.
