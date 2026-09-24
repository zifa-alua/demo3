# JWT-аутентификация

REST API для новостного сервиса на **Spring Boot + PostgreSQL** с полноценной
**JWT-аутентификацией**, разграничением доступа по ролям, ротацией
refresh-токенов и базовой защитой от brute-force.

Это продолжение TASK-002 (CRUD новостей): в TASK-003 добавлен слой безопасности.

## Стек технологий

- Java 17
- Spring Boot 4 (Spring Web, Spring Security 6, Spring Data JPA, Validation)
- PostgreSQL 16 (в Docker)
- jjwt (io.jsonwebtoken) для работы с JWT
- Maven

## Требования для запуска

- **JDK 17**
- **Docker Desktop** (для контейнера PostgreSQL)
- **IntelliJ IDEA** (или любая IDE / Maven)

## 1. Запуск базы данных (PostgreSQL в Docker)

Приложение ожидает PostgreSQL на **порту 5433**, база **`bitlab_news`**.

**Вариант А — docker-compose (рекомендуется).** В корне проекта лежит файл
`docker-compose.yml`. Из папки проекта выполни:

```bash
docker compose up -d
```

**Вариант Б — обычный docker run:**

```bash
docker run --name bitlab-postgres ^
  -e POSTGRES_USER=news_user ^
  -e POSTGRES_PASSWORD=localpass123 ^
  -e POSTGRES_DB=bitlab_news ^
  -p 5433:5432 ^
  -d postgres:16
```

> На macOS/Linux замени переносы строк `^` на `\`.

Проверить, что контейнер запущен: команда `docker ps` должна показать
`bitlab-postgres`.


## 2. Переменные окружения

Секреты **никогда** не хранятся в коде или в `application.yml` — они читаются из
переменных окружения. Задай их перед запуском приложения
(в IntelliJ: **Run → Edit Configurations → Environment variables**):

| Переменная    | Пример значения                                 | Описание                              |
|---------------|-------------------------------------------------|---------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5433/bitlab_news`  | JDBC-адрес базы данных                |
| `DB_USER`     | `news_user`                                     | Пользователь базы                     |
| `DB_PASSWORD` | `localpass123`                                  | Пароль базы                           |
| `JWT_SECRET`  | *(строка Base64 из 64 байт — сгенерируй свою)*  | Секретный ключ для подписи JWT-токенов |

**Сгенерировать JWT-секрет** (PowerShell):

```powershell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

> Без `JWT_SECRET` приложение **не запустится** — это сделано специально, чтобы
> случайно не использовать пустой или слабый секрет.

## 3. Запуск приложения

В IntelliJ нажми **Run** (▶️), либо из терминала:

```bash
./mvnw spring-boot:run
```

Приложение поднимется на **http://localhost:8080**. При первом запуске таблицы
(`users`, `refresh_tokens`, `news`) создаются автоматически.

## 4. Эндпоинты API

### Auth (публичные — токен не нужен)

| Метод | Эндпоинт         | Тело запроса                  | Успех |
|-------|------------------|-------------------------------|-------|
| POST  | `/auth/register` | `email`, `password`           | 201   |
| POST  | `/auth/login`    | `email`, `password`           | 200 → `accessToken`, `refreshToken` |
| POST  | `/auth/refresh`  | `refreshToken`                | 200 → новая пара токенов |
| POST  | `/auth/logout`   | `refreshToken`                | 200 (токен отзывается) |

### News (защищённые — нужен Bearer-токен)

| Метод  | Эндпоинт         | Кто может вызвать    | Успех |
|--------|------------------|----------------------|-------|
| GET    | `/api/news`      | любой авторизованный | 200   |
| POST   | `/api/news`      | **только ADMIN**     | 201   |
| PUT    | `/api/news/{id}` | **только ADMIN**     | 200   |
| DELETE | `/api/news/{id}` | **только ADMIN**     | 200/204 |

Access-токен передаётся в заголовке:

```
Authorization: Bearer <accessToken>
```

## 5. Роли

- **STUDENT** — роль по умолчанию для каждого нового пользователя. Может только
  читать новости (GET).
- **ADMIN** — может создавать / изменять / удалять новости.

При регистрации всегда назначается роль **STUDENT** (клиент не может выбрать роль
сам — это защита от повышения привилегий). Чтобы выдать ADMIN, обнови роль прямо
в базе:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'someone@test.kz';
```

После этого нужно заново залогиниться, чтобы получить токен с новой ролью.


## 6. Тестирование

Готовый набор запросов лежит в файле **`requests.http`** (открой его в IntelliJ и
жми зелёную стрелку у каждого запроса). Рекомендуемый порядок и ожидаемые коды:

| № | Запрос                          | Условие                         | Ожидается |
|---|---------------------------------|---------------------------------|-----------|
| 1 | POST `/auth/register`           | новый email                     | 201 (409, если уже есть) |
| 2 | POST `/auth/login`              | верный пароль                   | 200       |
| 3 | GET `/api/news`                 | с валидным access-токеном       | 200       |
| 4 | GET `/api/news`                 | без токена                      | 401       |
| 5 | POST `/api/news`                | токен STUDENT                   | 403       |
| 5'| POST `/api/news`                | токен ADMIN, все поля           | 201       |
| 6 | POST `/auth/refresh`            | валидный refresh-токен          | 200       |
| 7 | POST `/auth/logout`             | refresh-токен                   | 200       |
| 8 | POST `/auth/refresh`            | отозванный refresh-токен        | 401       |

Access-токен живёт **15 минут**, refresh-токен — **7 дней**, он одноразовый
(ротируется при каждом refresh, отзывается при logout).


## 7. Реализованные меры безопасности

- **Пароли** хранятся только в виде **BCrypt**-хеша — никакого plain-text.
- **JWT-секрет** задаётся только через переменную окружения `JWT_SECRET`, в
  репозиторий не попадает.
- **Алгоритм `none` отклоняется** — токены проверяются явным ключом подписи HS256;
  токен без подписи не проходит проверку и получает 401.
- **Refresh-токены** хранятся в базе в виде **SHA-256**-хешей, а не в открытом
  виде; ротируются при refresh и отзываются при logout.
- **Роль берётся из базы**, а не из payload токена — claim `role` не принимается
  на веру.
- **Защита от brute-force** — вход ограничен по частоте (5 попыток в минуту с
  одного клиента), при превышении возвращается **429**.
- **Невалидный / отсутствующий токен → 401** (без деталей); **чужая роль → 403**.
- При регистрации принудительно ставится роль **STUDENT** — защита от повышения
  привилегий.


## Структура проекта

```
src/main/java/com/example/demo/
├── controller/      # AuthController, NewsController
├── dto/             # DTO запросов и ответов
├── entity/          # User, Role, RefreshToken, News
├── exception/       # кастомные исключения + GlobalExceptionHandler
├── repository/      # репозитории Spring Data JPA
├── security/        # JwtUtil, JwtAuthenticationFilter, SecurityConfig,
│                    # PasswordConfig, TokenHashUtil, LoginRateLimiter,
│                    # RestAuthEntryPoint
└── service/         # AuthService, NewsService
```
