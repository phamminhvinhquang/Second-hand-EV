# STAGE 1: Giai đoạn Build (Sử dụng Maven và JDK)
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copy pom và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy mã nguồn
COPY src ./src

# 3. Build ứng dụng
RUN mvn package -DskipTests

# STAGE 2: Giai đoạn Run (Chỉ sử dụng JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build (SỬA TỪ 'build' THÀNH 'builder')
COPY --from=builder /app/target/*.jar app.jar

# *** LƯU Ý QUAN TRỌNG CHO CHAT SERVICE ***
VOLUME /app/uploads

# Port 8090
EXPOSE 8090

# Lệnh chạy
ENTRYPOINT ["java", "-jar", "/app/app.jar"]