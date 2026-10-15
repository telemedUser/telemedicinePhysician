# Gradle module :telemetry-service (port 8104)
#
# Build from telemedicine-backend root:
#   docker build -f services/telemetry-service/Dockerfile -t telemedicine/telemetry-service:0.1.0 .
#
# Equivalent shared build:
#   docker build -f docker/Dockerfile.spring-service \
#     --build-arg GRADLE_MODULE=telemetry-service \
#     --build-arg SERVICE_PORT=8104 \
#     -t telemedicine/telemetry-service:0.1.0 .

ARG GRADLE_MODULE=telemetry-service
ARG SERVICE_PORT=8104

FROM gradle:8.10.2-jdk21 AS build
ARG GRADLE_MODULE
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY build-logic ./build-logic
COPY shared ./shared
COPY gateway ./gateway
COPY platform ./platform
COPY bff ./bff
COPY services ./services
COPY simulators ./simulators
RUN gradle :${GRADLE_MODULE}:bootJar --no-daemon -x test \
    && JAR=$(find . -path "*/${GRADLE_MODULE}/build/libs/*.jar" ! -name "*-plain.jar" | head -1) \
    && test -n "$JAR" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_PORT=8104
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE ${SERVICE_PORT}
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://localhost:${SERVICE_PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
