FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# COPIE TOUT LE CONTENU DU DOSSIER BACK
COPY . .

# Build (le pom.xml et src/ sont déjà copiés)
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Crée le dossier uploads
RUN mkdir -p /app/uploads

ENV FILE_UPLOAD_DIR=/app/uploads

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]