# News CRUD Service — TASK-002

Локальный backend-сервис на Spring Boot для управления новостями (CRUD).

## Стек

- Java 17
- Spring Boot 4.1.1 (Web, Data JPA, Validation)
- PostgreSQL 16
- Maven

## Запуск локально

### 1. Поднять PostgreSQL через Docker

docker run --name bitlab-pg -e POSTGRES_DB=bitlab_news -e POSTGRES_USER=news_user -e POSTGRES_PASSWORD=localpass123 -p 5433:5432 -d postgres:16

Если контейнер уже создан, но остановлен:

docker start bitlab-pg

### 2. Задать переменные окружения

Смотри `.env.example`. Нужны три переменные: DB_URL, DB_USER, DB_PASSWORD.

При запуске через IntelliJ IDEA: Run → Edit Configurations → Environment variables.

### 3. Запустить приложение

Через IntelliJ: кнопка Run на конфигурации Demo3Application.

Либо через Maven:

mvn spring-boot:run

Приложение поднимется на http://localhost:8080

## API

| Метод  | Путь           | Описание                 |
|--------|----------------|---------------------------|
| POST   | /api/news      | Создать новость            |
| GET    | /api/news      | Получить список новостей   |
| GET    | /api/news/{id} | Получить новость по id     |
| PUT    | /api/news/{id} | Обновить новость            |
| DELETE | /api/news/{id} | Удалить новость             |

## Тестовые запросы

Все запросы для проверки CRUD-операций находятся в файле requests.http

## Меры безопасности, заложенные в код

- SQL Injection — используется только Spring Data JPA, без ручного построения SQL-запросов
- Mass Assignment — клиент передаёт данные только через DTO, Entity наружу не отдаётся
- Excessive Data Exposure — ответы API формируются через NewsResponseDto с ограниченным набором полей
- Утечка данных в ошибках — GlobalExceptionHandler возвращает только общие сообщения, без стектрейсов
- Хранение секретов — параметры БД передаются через переменные окружения, .env не коммитится
- Валидация входных данных — все поля DTO проверяются через Bean Validation