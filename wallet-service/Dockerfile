# ============================
# STAGE 1: Build ứng dụng
# ============================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml trước
COPY pom.xml .

# Tải dependencies
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source
COPY src ./src

# Build ứng dụng
RUN ./mvnw package -DskipTests


# ============================
# STAGE 2: Run ứng dụng
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
