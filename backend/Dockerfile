# syntax=docker/dockerfile:1
# ---------- Étape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Réessais et timeouts plus tolérants pour les connexions instables
ENV MAVEN_ARGS="-B -Daether.connector.http.retryHandler.count=10 -Daether.connector.connectTimeout=60000 -Daether.connector.requestTimeout=300000"
COPY pom.xml .
# Le cache ~/.m2 survit aux échecs : un nouveau build reprend là où il s'est arrêté
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

# ---------- Étape 2 : exécution ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/huilerie-erp.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
