FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN gradle :server:installDist --no-daemon
RUN mv /app/server/build/install/server/lib/server.jar /app/server/build/install/server/server.jar

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=build /app/server/build/install/server/bin/ ./bin/
COPY --from=build /app/server/build/install/server/lib/ ./lib/
COPY --from=build /app/server/build/install/server/server.jar ./lib/server.jar

EXPOSE 8147
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
