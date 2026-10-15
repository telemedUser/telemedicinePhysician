# Telemedicine Platform — OpenAPI Specification Index

All service contracts are OpenAPI 3.1 YAML files under `telemedicine-backend/shared/contracts/`.

## Published contracts

| Service | Contract file |
|---------|---------------|
| Common error model | `common-error.yaml` |
| Patient BFF | `patient-bff.yaml` |
| Waiting room | `waiting-room-service.yaml` |
| Template (Java) | `template-java.yaml` |
| Template (Python) | `template-python.yaml` |

## Event catalogue

Asynchronous contracts are documented in `shared/contracts/events/event-catalogue.yaml` (28 event types).

## Gateway aggregation

In production, the gateway exposes aggregated OpenAPI at:

```
GET https://api.example.com/v3/api-docs
```

## Code generation

Frontend `shared-types` and `api-client` packages are designed to consume generated clients from these specs. Run generation in CI when contracts change:

```bash
# Example (wire into backend CI)
openapi-generator-cli generate -i shared/contracts/patient-bff.yaml -g typescript-fetch -o ../telemedicine-frontend/packages/shared-types/src/generated
```

## Contract testing

Use `java-common-testing` WireMock stubs pointed at `external-simulators` together with OpenAPI contract tests per service.
