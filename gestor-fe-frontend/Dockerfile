# =========================================================================
# Etapa 1: Compilación de la aplicación Angular 17
# =========================================================================
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .
RUN npm run build -- --configuration production

# =========================================================================
# Etapa 2: Servidor Web Nginx para producción
# =========================================================================
FROM nginx:alpine

# Copiar el artefacto compilado al document root de Nginx
COPY --from=build /app/dist/gestor-fe-frontend/browser /usr/share/nginx/html

# Copiar la configuración personalizada de Nginx para SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Zona horaria
ENV TZ=America/Bogota

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
