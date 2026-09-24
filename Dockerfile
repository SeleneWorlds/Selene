FROM node:24.16.0-bookworm AS node
RUN npm install --global pnpm@11.5.2

FROM gradle:9.3-jdk25 AS build
COPY --from=node /usr/local/ /usr/local/
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN pnpm --dir client-web install --frozen-lockfile
RUN gradle :server:installDist --no-daemon
RUN mv /app/server/build/install/server/lib/server.jar /app/server/build/install/server/server.jar

FROM eclipse-temurin:25-alpine
WORKDIR /app
COPY --from=build /app/server/build/install/server/bin/ ./bin/
COPY --from=build /app/server/build/install/server/lib/ ./lib/
COPY --from=build /app/server/build/install/server/server.jar ./lib/server.jar

EXPOSE 8147
EXPOSE 8148
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
