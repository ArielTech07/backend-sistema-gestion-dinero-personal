# ==============================================================================
# ETAPA 1: Construcción (Build) con Maven y JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiamos la configuración de dependencias primero para aprovechar el cache de Docker
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Aseguramos permisos de ejecución en sistemas Unix (Render corre sobre Linux)
RUN chmod +x mvnw

# Descargamos dependencias en cache
RUN ./mvnw dependency:go-offline -B

# Copiamos el código fuente y compilamos el JAR sin correr tests durante el empaquetado
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# ETAPA 2: Imagen Final Liviana para Producción (JRE 21 Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Creamos un usuario no privilegiado por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiamos solo el JAR compilado de la etapa 1
COPY --from=builder /app/target/*.jar app.jar

# Render asigna dinámicamente la variable PORT (por defecto 8080 si no está definida)
ENV PORT=8080
EXPOSE 8080

# Parámetros optimizados de memoria JVM para el nivel gratuito de Render (512MB RAM)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

