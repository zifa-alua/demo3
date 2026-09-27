# BITLAB News API — авторизация и роли

REST API новостного сервиса на **Spring Boot + PostgreSQL** с полноценной
**JWT-аутентификацией**, разграничением доступа по ролям (Spring Security),
ротацией refresh-токенов и базовой защитой от brute-force.

Развитие проекта по спринтам:
- **TASK-002** — CRUD новостей
- **TASK-003** — слой безопасности (JWT, регистрация/логин/refresh/logout)
- **TASK-005** — роли через `@PreAuthorize`, эндпоинт профиля `/api/users/me`,
  корректные коды 401/403

---

## Стек технологий

- Java 17
- Spring Boot 4 (Spring Web, Spring Security 6, Spring Data JPA, Validation)
- PostgreSQL 16 (в Docker)
- jjwt (io.jsonwebtoken) для работы с JWT
- Maven

---

## Требования для запуска

- **JDK 17**
- **Docker Desktop** (для контейнера PostgreSQL)
- **IntelliJ IDEA** (или любая IDE / Maven)

---

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

---

## 2. Переменные окружения

Секреты **никогда** не хранятся в коде — только в переменных окружения
(в IntelliJ: **Run → Edit Configurations → Environment variables**):

| Переменная    | Пример значения                                 | Описание                              |
|---------------|-------------------------------------------------|---------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5433/bitlab_news`  | JDBC-адрес базы                       |
| `DB_USER`     | `news_user`                                     | Пользователь базы                     |
| `DB_PASSWORD` | `localpass123`                                  | Пароль базы                           |
| `JWT_SECRET`  | *(строка Base64 из 64 байт — сгенерируй свою)*  | Секретный ключ для подписи JWT        |

**Сгенерировать JWT-секрет** (PowerShell):

```powershell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

> Без `JWT_SECRET` приложение не запустится — это защита от пустого/слабого секрета.

---

## 3. Запуск приложения

В IntelliJ нажми **Run** (▶️), либо `./mvnw spring-boot:run`.
Приложение поднимется на **http://localhost:8080**. Таблицы создаются автоматически.

---

## 4. Как получить токен

1. **Регистрация:** `POST /auth/register` с телом `{ "email": "...", "password": "..." }` → 201.
   Каждый новый пользователь получает роль **STUDENT**.
2. **Вход:** `POST /auth/login` с теми же email/паролем → 200 и JSON:
   ```json
   { "accessToken": "eyJ...", "refreshToken": "..." }
   ```
3. Полученный `accessToken` передавай в заголовке к защищённым запросам:
   ```
   Authorization: Bearer <accessToken>
   ```
4. Access-токен живёт **15 минут**. Когда истечёт — обнови его через
   `POST /auth/refresh` с телом `{ "refreshToken": "..." }` (refresh живёт 7 дней).

---

## 5. Роли и права

| Роль    | Права                                                        |
|---------|--------------------------------------------------------------|
| STUDENT | Чтение новостей (GET). Роль по умолчанию при регистрации.     |
| ADMIN   | Чтение + создание, редактирование, удаление новостей.        |

Роль назначается только сервером (клиент не может выбрать её при регистрации).
Чтобы выдать ADMIN, обнови роль в базе:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'someone@test.kz';
```

Разграничение реализовано двумя уровнями: правилами в `SecurityConfig` и
аннотациями `@PreAuthorize("hasRole('ADMIN')")` на методах контроллера.

---

## 6. Эндпоинты API

### Auth (публичные — токен не нужен)

| Метод | Эндпоинт         | Тело / описание                             |
|-------|------------------|---------------------------------------------|
| POST  | `/auth/register` | `email`, `password` → 201                   |
| POST  | `/auth/login`    | `email`, `password` → accessToken + refreshToken |
| POST  | `/auth/refresh`  | `refreshToken` → новый accessToken          |
| POST  | `/auth/logout`   | `refreshToken` → отзывается токен            |

### Защищённые (нужен Bearer-токен)

| Метод  | Эндпоинт         | Кто может            |
|--------|------------------|----------------------|
| GET    | `/api/news`      | любой авторизованный |
| GET    | `/api/news/{id}` | любой авторизованный |
| POST   | `/api/news`      | только ADMIN         |
| PUT    | `/api/news/{id}` | только ADMIN         |
| DELETE | `/api/news/{id}` | только ADMIN         |
| GET    | `/api/users/me`  | любой авторизованный (свой профиль из токена) |

---

## 7. Коды ответов авторизации

- **401 Unauthorized** — токена нет, он невалиден или истёк.
- **403 Forbidden** — токен валиден, но роли не хватает (например, STUDENT пытается создать новость).

---

## 8. Меры безопасности

- Пароли хранятся только как **BCrypt**-хеши.
- **JWT-секрет** — только в переменной окружения, не в репозитории.
- Алгоритм `none` отклоняется (проверка подписи HS256).
- Refresh-токены хранятся как **SHA-256**-хеши, ротируются при refresh и отзываются при logout.
- Роль берётся из БД, а не из payload токена.
- Ограничение частоты входа (5 попыток/мин) → 429.
- CORS разрешён только для origin фронтенда.
- Эндпоинт `/api/users/{id}` не реализован (только `/me`) — IDOR невозможен.
