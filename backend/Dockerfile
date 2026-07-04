# ---- Build stage ----
# Builds the Spring Boot fat jar with Maven and a JDK 17 toolchain.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the application (tests run in CI, not in the image build).
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Runtime stage ----
# Slim JRE-only image for a smaller, faster-starting container.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar (the -exec/plain classifier is excluded by the wildcard order).
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT; Spring reads it via server.port=${PORT:8080}.
EXPOSE 8080

# MaxRAMPercentage keeps the JVM within Render's free 512 MB instance.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
