# ============================
# STAGE 1: Build ứng dụng
# ============================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml (load dependency trước)
COPY pom.xml .

# Tải dependencies
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source
COPY src ./src

# ❗❗ Build + repackage để tạo JAR có Main-Class
RUN ./mvnw clean package spring-boot:repackage -DskipTests


# ============================
# STAGE 2: Run ứng dụng
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy file jar build từ stage 1
COPY --from=builder /app/target/*.jar app.jar

# Expose port Gateway
EXPOSE 9000

# Run ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
