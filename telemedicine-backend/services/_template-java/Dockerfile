FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY shared/java-common ./shared/java-common
COPY services/_template-java ./services/_template-java
RUN gradle :template-java:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/_template-java/build/libs/template-java.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
