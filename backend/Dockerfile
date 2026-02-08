# ── Stage 1: build ──────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (skip tests – they ran in CI)
COPY src ./src
RUN mvn package -DskipTests -B \
    && mv target/*.jar target/app.jar

# ── Stage 2: runtime ───────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/target/app.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
