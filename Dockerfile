# ===== STAGE 1: Build =====
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos solo lo necesario (pom.xml primero para cachear dependencias)
COPY pom.xml .
RUN mvn -q -Dmaven.test.skip=true dependency:go-offline dependency:resolve-plugins

# Copiamos el código fuente
COPY src ./src

# Compilamos y empaquetamos
RUN mvn -Dmaven.test.skip=true package

# ===== STAGE 2: Run =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crear usuario no-root
RUN useradd -ms /bin/bash appuser
USER appuser

# Copiar el jar
COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar

# Variables por defecto
ENV SERVER_PORT=8080 SPRING_PROFILES_ACTIVE=prod JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Arranque
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]