# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx gettext-base curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf

WORKDIR /app
COPY --from=backend /build/target/aegisterra-platform-1.0.0.jar /app/app.jar
COPY --from=frontend /web/dist /usr/share/nginx/html
COPY deploy/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY deploy/start.sh /app/start.sh
RUN sed -i 's/\r$//' /app/start.sh /etc/nginx/templates/default.conf.template \
    && chmod +x /app/start.sh

ENV PORT=80 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD sh -c 'curl -fsS "http://127.0.0.1:${PORT:-80}/api/v1/health" || exit 1'

CMD ["/app/start.sh"]
