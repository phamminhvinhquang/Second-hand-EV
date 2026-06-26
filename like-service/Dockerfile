# ---------- STAGE 1: build with Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# copy pom + sources (maven cache tốt hơn khi copy pom trước)
COPY pom.xml ./
COPY src ./src

# package (skip tests to speed up; bỏ -DskipTests nếu muốn chạy test)
RUN mvn -B -DskipTests package

# ---------- STAGE 2: runtime ----------
FROM eclipse-temurin:21-jre
# tạo user không-root
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app

# copy jar từ stage builder (wildcard match artifact)
COPY --from=builder /workspace/target/*.jar app.jar

# quyền và user
RUN chown app:app /app/app.jar
USER app

# port theo application.properties
EXPOSE 8088

# cho phép override JVM opts
ENV JAVA_OPTS=""

# entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
