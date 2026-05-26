FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY services/common/ services/common/
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/project-service/pom.xml services/project-service/pom.xml
COPY services/report-service/pom.xml services/report-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/execution-service/ services/execution-service/
RUN mvn clean package -pl services/execution-service -am -DskipTests -q

# Use full JRE (not alpine) to ensure compatibility with Gatling's native libs
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip && rm -rf /var/lib/apt/lists/*

# Install Gatling from pre-downloaded bundle (avoids slow runtime download)
ARG GATLING_VERSION=3.10.5
COPY services/execution-service/gatling-bundle.zip /tmp/gatling.zip
RUN unzip -q /tmp/gatling.zip -d /tmp/gatling-extract \
    && mkdir -p /opt/gatling \
    && mv /tmp/gatling-extract/gatling-charts-highcharts-bundle-${GATLING_VERSION}/* /opt/gatling/ \
    && rm -rf /tmp/gatling.zip /tmp/gatling-extract

COPY --from=builder /workspace/services/execution-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
