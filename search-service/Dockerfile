# Dockerfile
# stage 1: build with Maven + JDK21
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app

# copy maven files first to leverage layer cache
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ["mvn", "-B", "dependency:go-offline"]

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# stage 2: runtime (JRE 21)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java","-jar","/app/app.jar"]
