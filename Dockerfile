# ── Stage 1: Build with Maven ─────────────────────────────────
FROM maven:3.9-eclipse-temurin-24-alpine AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S drivex && adduser -S drivex -G drivex

COPY --from=build /build/target/drivex-backend-*.jar app.jar
RUN chown drivex:drivex app.jar
USER drivex

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
