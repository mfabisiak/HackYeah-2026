# Build context: repository root (see docker-compose.yml)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# The project also contains Android/iOS modules; only the server distribution is built here.
RUN ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server/ ./
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
