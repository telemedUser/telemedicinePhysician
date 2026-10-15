# Telemedicine Backend Monorepo

Polyglot microservices backend with Spring Cloud Gateway, Consul service discovery, and template services in Java and Python.

## Structure

- `gateway/` — Spring Cloud Gateway (public API entry)
- `services/_template-java/` — Spring Boot microservice template
- `services/_template-python/` — FastAPI microservice template
- `shared/java-common/` — Shared Java DTOs
- `shared/python-common/` — Shared Python package (pip editable install)
- `shared/contracts/` — OpenAPI specifications
- `deploy/` — Docker Compose and Kubernetes manifests

## Quick start (Docker)

```bash
make up
curl http://localhost:8080/api/v1/template-java/hello
curl http://localhost:8080/api/v1/template-python/hello
```

Consul UI: http://localhost:8500

## Local Python development

```bash
pip install -r shared/python-common/requirements.txt
pip install -r services/_template-python/requirements.txt
pip install -r services/_template-python/requirements-dev.txt
make test-python
```

## Scaffold new services

```bash
make scaffold-java NAME=patient-service
make scaffold-python NAME=appointment-service
```

Then register the service in `gateway/src/main/resources/application.yml` and `settings.gradle` (Java only).

## Build orchestration

| Stack | Tool |
|-------|------|
| Java | Gradle multi-module |
| Python | pip + requirements.txt per service |
| Cross-language | Makefile |
