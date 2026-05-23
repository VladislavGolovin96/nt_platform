# Load Testing Platform

Веб-платформа для запуска и мониторинга нагрузочного тестирования на базе Gatling.

## Быстрый старт

### 1. Клонировать репозиторий и настроить окружение

```bash
git clone <repo-url>
cd nt_platform
cp .env.example .env
# Отредактируй .env — минимум задай JWT_SECRET (32+ символа)
```

### 2. Запустить инфраструктуру

```bash
docker-compose up -d
```

Дождаться пока все healthcheck станут зелёными (~60 сек):

```bash
docker-compose ps
```

### 3. Запустить бэкенд-сервисы (в отдельных терминалах)

```bash
# Из корня монорепо
cd services/auth-service    && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/project-service && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/execution-service && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/report-service  && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=docker
cd services/api-gateway     && mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### 4. Запустить фронтенд

```bash
cd frontend
npm install
npm run dev
```

## Доступные сервисы

| Сервис          | URL                          |
|-----------------|------------------------------|
| Frontend        | http://localhost:5173        |
| API Gateway     | http://localhost:8080        |
| Grafana         | http://localhost:3000        |
| Kafka UI        | http://localhost:8090        |
| MinIO Console   | http://localhost:9001        |
| Mailhog UI      | http://localhost:8025        |
| Prometheus      | http://localhost:9090        |

**Grafana** логин: `admin` / `admin`  
**MinIO** логин: `minioadmin` / `minioadmin`

## Архитектура

Смотри [ARCHITECTURE.md](ARCHITECTURE.md) и [CLAUDE.md](CLAUDE.md).

## Сборка всех сервисов

```bash
mvn clean package -DskipTests
```

## Остановка

```bash
docker-compose down
# Удалить volumes (данные БД):
docker-compose down -v
```
