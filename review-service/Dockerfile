# STAGE 1: Giai đoạn Build (Dùng Maven Java 21)
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copy pom và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy mã nguồn
COPY src ./src

# 3. Build ứng dụng
RUN mvn package -DskipTests

# STAGE 2: Giai đoạn Run (Dùng JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ stage 'builder'
COPY --from=builder /app/target/*.jar app.jar

# Port (Cấu hình port cho đúng với service này)
EXPOSE 8086

# Lệnh chạy
ENTRYPOINT ["java", "-jar", "/app/app.jar"]