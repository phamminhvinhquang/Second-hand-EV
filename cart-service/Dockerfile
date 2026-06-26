# Dockerfile
# ---------- STAGE 1: build with Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set working dir and copy sources
WORKDIR /workspace
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for speed)
RUN mvn -B -DskipTests package

# ---------- STAGE 2: runtime ----------
FROM eclipse-temurin:21-jre

# Create non-root user for running the app
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app

# Copy fat/executable JAR from builder stage.
# This uses a wildcard so it works with default maven/boot jar naming: <artifactId>-<version>.jar
COPY --from=builder /workspace/target/*.jar app.jar

# Ensure permissions and switch to non-root user
RUN chown app:app /app/app.jar
USER app

# Expose port configured in application.properties
EXPOSE 8082

# Allow passing JVM options through environment variable JAVA_OPTS
ENV JAVA_OPTS=""

# Start the app
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
