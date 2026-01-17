FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copie d'abord SEULEMENT le pom.xml
COPY pom.xml .

# Télécharge les dépendances
RUN mvn dependency:go-offline -B

# Maintenant copie le reste (src/)
COPY src ./src

# Build
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Crée le dossier uploads
RUN mkdir -p /app/uploads

ENV FILE_UPLOAD_DIR=/app/uploads

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]