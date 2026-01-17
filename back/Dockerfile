# Étape 1 : Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Crée le dossier de travail
WORKDIR /workspace/app

# Copie le pom.xml
COPY pom.xml .

# Télécharge toutes les dépendances
RUN mvn dependency:go-offline -B

# Copie le code source
COPY src src

# Build l'application
RUN mvn clean package -DskipTests

# Étape 2 : Runtime
FROM eclipse-temurin:17-jre-alpine

# Crée l'utilisateur non-root pour sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copie le JAR depuis l'étape de build
COPY --from=builder /workspace/app/target/*.jar app.jar

# Crée le dossier pour uploads
RUN mkdir -p /app/uploads

# Variables d'environnement
ENV FILE_UPLOAD_DIR=/app/uploads \
    SPRING_PROFILES_ACTIVE=prod

# Port exposé
EXPOSE 8081

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]