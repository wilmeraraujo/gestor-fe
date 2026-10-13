# =========================================================================
# Dockerfile: gestor-fe-admin-service (Spring Boot)
# Puerto: 9095
# =========================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Zona horaria de Colombia
ENV TZ=America/Bogota
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# Directorio de logs
RUN mkdir -p /app/logs

# Copiar el paquete compilado (Spring Boot executable WAR)
COPY target/*.war app.war

EXPOSE 9095

ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
