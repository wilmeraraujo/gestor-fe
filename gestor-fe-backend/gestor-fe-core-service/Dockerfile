# =========================================================================
# Dockerfile: gestor-fe-core-service (Spring Boot)
# Puerto: 9096
# =========================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Zona horaria de Colombia
ENV TZ=America/Bogota
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# Directorios de logs y almacenamiento de soportes documentales
RUN mkdir -p /app/logs /opt/storage/gestion-fe /opt/storage/gestion-fe-validos

# Copiar el paquete compilado (Spring Boot executable WAR)
COPY target/*.war app.war

EXPOSE 9096

ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
