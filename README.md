# Load Testing Platform

Веб-платформа для запуска и мониторинга нагрузочного тестирования на базе Gatling.  
Пользователь указывает Git-репозиторий с Gatling-скриптами, выбирает тип теста, запускает его
в реальном времени наблюдает за логами и получает PDF-отчёт по завершении.

GITHUB - PROJECT : https://github.com/VladislavGolovin96/nt_platform

## Быстрый старт (полный стек через docker-compose)

### 1. Клонировать репозиторий

```bash
git clone <repo-url>
cd nt_platform
```

### 2. Настроить окружение

Файл `.env` уже создан с дефолтными значениями для локальной разработки.  
**Обязательно** смените `JWT_SECRET` перед использованием в production:

```bash
# Сгенерировать 32-байтовый секрет:
openssl rand -base64 32
```

Отредактируйте `.env`:
```
JWT_SECRET=<ваш-секрет-минимум-32-символа>
```

### 3. Собрать образы и запустить всё

```bash
docker-compose up -d --build
```

Первая сборка займёт ~5-10 минут (Maven скачает зависимости, соберёт все JAR-ы).

Дождаться пока все сервисы станут healthy (~2-3 минуты после сборки):

```bash
docker-compose ps
```

Все сервисы должны показывать `healthy`. Execution Service дольше остальных — он скачивает
Gatling bundle при первом старте (~100 MB).

### 4. Запустить фронтенд (dev server)

```bash
cd frontend
npm install
npm run dev
```

Открыть http://localhost:5173

---

## Пользовательский флоу

```
1. Регистрация  → /register
2. Логин        → /login
3. Создать проект (указать Git URL с Gatling-скриптами) → /projects/new
4. Дождаться статуса READY (сборка занимает 1-3 мин) → /projects/{id}
5. Запустить тест (выбрать Simulation class, тип теста) → /executions/{id}
6. Наблюдать логи в реальном времени (SSE terminal)
7. После завершения → View Report → Download PDF
```

---

## Доступные сервисы

| Сервис             | URL                        | Назначение                     |
|--------------------|----------------------------|--------------------------------|
| Frontend           | http://localhost:5173      | React SPA                      |
| API Gateway        | http://localhost:8080      | Единая точка входа             |
| Grafana            | http://localhost:3000      | Метрики и дашборды             |
| Kafka UI           | http://localhost:8090      | Мониторинг очередей            |
| MinIO Console      | http://localhost:9001      | Хранилище артефактов и PDF     |
| Mailhog UI         | http://localhost:8025      | Перехват email-уведомлений     |
| Prometheus         | http://localhost:9090      | Метрики сервисов               |

**Grafana** логин: `admin` / `admin`  
**MinIO** логин: `minioadmin` / `minioadmin`  
**Kafka UI** — без авторизации

---

## Архитектура

```
                    ┌──────────────┐
  Browser ─────────►  API Gateway  :8080
                    └──────┬───────┘
                           │  JWT validate / rate limit / route
         ┌─────────────────┼──────────────────────┐
         ▼                 ▼                       ▼
   auth-service      project-service       execution-service
     :8081              :8082                   :8083
                                                  │ SSE logs
                    report-service      notification-service
                       :8084                   :8085

  Kafka topics: project.built, test.started, test.log.*, test.finished, test.failed, report.generated
```

Подробнее: [ARCHITECTURE.md](ARCHITECTURE.md) · [CLAUDE.md](CLAUDE.md)

---

## Сборка без Docker

```bash
# Установить инфраструктуру
docker-compose up -d postgres mongodb redis kafka minio grafana prometheus mailhog

# Собрать все JAR-ы
mvn clean package -DskipTests

# Запустить каждый сервис в отдельном терминале
cd services/auth-service         && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/project-service      && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/execution-service    && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/report-service       && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/api-gateway          && mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

---

## Управление стеком

```bash
# Остановить всё
docker-compose down

# Остановить и удалить все данные (БД, MinIO, Kafka offset-ы)
docker-compose down -v

# Пересобрать один сервис
docker-compose build execution-service
docker-compose up -d execution-service

# Посмотреть логи сервиса
docker-compose logs -f execution-service

# Проверить healthcheck статусы
docker-compose ps
```

---

## Стек технологий

| Слой           | Технология                                    |
|----------------|-----------------------------------------------|
| Backend        | Spring Boot 3.3.x, Java 21, Maven 3.9.x       |
| Gateway        | Spring Cloud Gateway 4.x + Bucket4j           |
| Messaging      | Apache Kafka 7.6 (KRaft mode)                 |
| Auth           | Spring Security 6, JWT (jjwt 0.12.x)          |
| RDBMS          | PostgreSQL 16 + Flyway                        |
| NoSQL          | MongoDB 7 (execution logs)                    |
| Cache          | Redis 7 / Valkey (rate limit, artifact cache) |
| Object Storage | MinIO (артефакты сборки, PDF-отчёты)          |
| Load Testing   | Gatling 3.10.x (запускается как внешний процесс) |
| PDF            | iText 7                                       |
| Frontend       | React 18, Vite 5, React Query v5, Tailwind    |
| Observability  | Prometheus + Grafana                          |
