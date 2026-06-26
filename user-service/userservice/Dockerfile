# ----------------------------------------------------
# GIAI ĐOẠN 1: BUILD (Sử dụng Maven và JDK 21)
# ----------------------------------------------------
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Đặt thư mục làm việc bên trong container build
WORKDIR /app

# Sao chép các file cấu hình Maven và mã nguồn
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

# Chạy build (tạo file JAR)
RUN mvn clean package -DskipTests

# ----------------------------------------------------
# GIAI ĐOẠN 2: RUN (Sử dụng JRE 21 nhẹ để chạy ứng dụng)
# ----------------------------------------------------

# Sử dụng JRE 21 nhẹ hơn
FROM eclipse-temurin:21-jre-alpine

# Đặt biến môi trường cho Spring
ENV JAR_FILE=target/userservice-0.0.1-SNAPSHOT.jar

# Đặt thư mục làm việc chính
WORKDIR /app

# Sao chép file JAR từ giai đoạn 'build' vào giai đoạn 'run'
COPY --from=build /app/${JAR_FILE} app.jar

# Expose cổng 8084 (theo application.properties)
EXPOSE 8084

# Lệnh chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]