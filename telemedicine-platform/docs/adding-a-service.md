# Adding a Backend Microservice

## Java service

1. Scaffold from template:

```bash
cd telemedicine-backend
make scaffold-java NAME=patient-service
```

2. Register in Gradle — add to `settings.gradle`:

```gradle
include 'patient-service'
project(':patient-service').projectDir = file('services/patient-service')
```

3. Add gateway route in `gateway/src/main/resources/application.yml`:

```yaml
- id: patient-service
  uri: lb://patient-service
  predicates:
    - Path=/api/v1/patient-service/**
  filters:
    - StripPrefix=3
```

4. Set `spring.application.name: patient-service` in the service's `application.yml`.

5. Use the **schema-per-service** database from `infra/postgres/initdb/01-databases.sql` (e.g. `DB_NAME=waiting_room`, `DB_USER=waiting_room`).

6. Enable Flyway with shared outbox migrations:

```yaml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/migration/common
  jpa:
    hibernate:
      ddl-auto: validate
```

7. Add `@EnableTelemedicinePersistence` on the application class. Do **not** add a custom `/health` controller — use Actuator only.

8. Add OpenAPI contract in `shared/contracts/patient-service.yaml`.

6. Add Dockerfile, K8s Deployment/Service under `deploy/kubernetes/base/patient-service/`.

7. Build and deploy independently:

```bash
docker build -f services/patient-service/Dockerfile -t telemedicine/patient-service:0.1.0 .
```

## Python service

1. Scaffold:

```bash
make scaffold-python NAME=appointment-service
```

2. Update `requirements.txt` (editable python-common path is pre-configured).

3. Set `SERVICE_NAME=appointment-service` in the service's `main.py`.

4. Add gateway route (same pattern as Java, `StripPrefix=3`).

5. Register Consul on startup using `telemedicine_common.consul_registry.register_service`.

6. Add contract, Dockerfile, and K8s manifests.

## Verification

```bash
curl http://localhost:8080/api/v1/patient-service/hello
```

Check Consul UI at http://localhost:8500 — service should appear as healthy.

## Adding a vendor simulator endpoint

When a new external system is integrated:

1. Add controller under `simulators/external-simulators` calling `SimulatorEngine.preProcess(VendorNames.X, scenarioHeader)`.
2. Seed defaults in `application.yml` under `telemedicine.simulator.vendors`.
3. Add enriched response helpers in `SimulatorResponses` and mirror in `java-common-testing` `SimulatorResponseBodies`.
4. Extend `SimulatorStubs.registerAll()` and optional scenario stub method.
5. Document scenarios in `docs/simulator-scenarios.md` and extend `shared/contracts/external-simulators.yaml`.

---

## Adding a frontend package

1. Create under `telemedicine-frontend/packages/<name>/` with:
   - `src/index.ts` or `index.tsx` (barrel exports)
   - `package.json` with `"private": true` and `workspace:*` deps
   - `project.json` with `lint` and `test` Nx targets
   - `vitest.config.ts` using `createVitestConfig` from root `vitest.shared.ts`
   - At least one `*.test.ts(x)` file

2. Register path alias in `tsconfig.base.json` and `vite.aliases.mts`.

3. For UI components, add Storybook stories under `design-system` or the owning package.

4. Wire workspace deps in consuming apps:
   ```bash
   node tools/wire-workspace-deps.mjs
   ```

5. Verify: `pnpm --filter @telemedicine/<name> test`

6. Run full Part 5 gate: `telemedicine-platform/scripts/verify-part5.ps1`
