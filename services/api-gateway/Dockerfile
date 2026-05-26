FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY services/common/ services/common/
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/project-service/pom.xml services/project-service/pom.xml
COPY services/execution-service/pom.xml services/execution-service/pom.xml
COPY services/report-service/pom.xml services/report-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/api-gateway/ services/api-gateway/
RUN mvn clean package -pl services/api-gateway -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/services/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
