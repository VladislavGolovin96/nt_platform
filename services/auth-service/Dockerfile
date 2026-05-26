FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY services/common/ services/common/
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/project-service/pom.xml services/project-service/pom.xml
COPY services/execution-service/pom.xml services/execution-service/pom.xml
COPY services/report-service/pom.xml services/report-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/auth-service/ services/auth-service/
RUN mvn clean package -pl services/auth-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/services/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
