# STAGE 1: Giai đoạn Build (Sử dụng Maven và JDK 21)
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copy pom và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy mã nguồn
COPY src ./src

# 3. Build ứng dụng
RUN mvn package -DskipTests

# STAGE 2: Giai đoạn Run (Chỉ sử dụng JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build
COPY --from=builder /app/target/*.jar app.jar

# Port 8085
EXPOSE 8085

# Lệnh chạy
ENTRYPOINT ["java", "-jar", "/app/app.jar"]