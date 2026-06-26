# STAGE 1: Giai đoạn Build (Sử dụng Maven và JDK 21)
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Đặt thư mục làm việc
WORKDIR /app

# 1. Copy file pom.xml và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy toàn bộ mã nguồn
COPY src ./src

# 3. Build ứng dụng, bỏ qua test
RUN mvn package -DskipTests

# STAGE 2: Giai đoạn Run (Chỉ sử dụng JRE 21)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy file .jar đã được build từ stage 1
COPY --from=builder /app/target/*.jar app.jar

# Port 8080
EXPOSE 8080

# Lệnh để chạy ứng dụng
ENTRYPOINT ["java", "-jar", "/app/app.jar"]