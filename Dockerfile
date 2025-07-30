FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN gradle build --no-daemon

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/server/build/libs/*.jar app.jar
EXPOSE 8147
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
