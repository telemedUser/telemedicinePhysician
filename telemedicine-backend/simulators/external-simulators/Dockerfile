# Multi-stage build — external vendor simulators
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY shared/java-common-core ./shared/java-common-core
COPY shared/java-common-web ./shared/java-common-web
COPY simulators/external-simulators ./simulators/external-simulators
RUN gradle :external-simulators:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/simulators/external-simulators/build/libs/external-simulators.jar app.jar
USER app
EXPOSE 8200
ENTRYPOINT ["java", "-jar", "app.jar"]
