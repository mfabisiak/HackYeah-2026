# Build context: repository root (see docker-compose.yml)

# 1) Kotlin/JS API client (:web-client) built by Gradle
FROM eclipse-temurin:21-jdk AS client
WORKDIR /src
COPY . .
# Windows checkouts may bring CRLF line endings, which break the gradlew shebang
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew :web-client:jsBrowserProductionLibraryDistribution --no-daemon

# 2) React app; it depends on the client through the relative path "file:../web-client/..."
FROM node:24-alpine AS app
WORKDIR /src/web
COPY --from=client /src/web-client/build/dist/js/productionLibrary /src/web-client/build/dist/js/productionLibrary
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
ARG VITE_KEYCLOAK_URL=http://localhost:8081
ARG VITE_KEYCLOAK_REALM=hubmi
ARG VITE_KEYCLOAK_CLIENT_ID=hubmi-app
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
RUN npm run build

# 3) Static files + reverse proxy to the API (same origin, so no CORS)
FROM nginx:1-alpine
COPY web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=app /src/web/dist /usr/share/nginx/html
EXPOSE 80
