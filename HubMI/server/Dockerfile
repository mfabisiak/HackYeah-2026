# Build context: repository root (see docker-compose.yml)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# Only the server distribution is built here (the React app in web/ is not part of the image).
# Windows checkouts may bring CRLF line endings, which break the gradlew shebang
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server/ ./
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
