# Build stage — no local JDK/Maven required, only Docker.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first for dependency-layer caching, then go offline.
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline

COPY src ./src
RUN mvn -q -e -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /build/target/whatsapp-meta-bot-*.jar app.jar

# Run as a non-root user.
RUN useradd --system --uid 1001 spring
USER spring

ENV PORT=3000
EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
