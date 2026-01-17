FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Just set environment variable - directory will be created by Java code
ENV FILE_UPLOAD_DIR=/app/uploads

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]