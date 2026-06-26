# Dockerfile
# ---------- STAGE 1: build with Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy maven files first for better caching
COPY pom.xml ./
# copy sources
COPY src ./src

# Build the project (skip tests to speed up)
RUN mvn -B -DskipTests package

# ---------- STAGE 2: runtime ----------
FROM eclipse-temurin:21-jre
# create non-root user
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app

# Copy built jar from builder stage. Uses wildcard to match artifactId-version.jar
COPY --from=builder /workspace/target/*.jar app.jar

# ensure permissions
RUN chown app:app /app/app.jar
USER app

# expose port used by the app
EXPOSE 8096

# allow custom JVM options
ENV JAVA_OPTS=""

# entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
