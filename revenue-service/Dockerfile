# Dockerfile 
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy pom & download dependencies first (cache friendly)
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy source và build
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: runtime (slim JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user non-root
RUN addgroup -S app && adduser -S -G app app

# Copy jar từ stage build
COPY --from=builder /workspace/target/*.jar app.jar

# Quyền cho user app
RUN chown app:app /app/app.jar
USER app

# Expose port (match application.properties server.port)
EXPOSE 8097

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
