# Removing the datastore (zero impact on APIs)

When all services use PostgreSQL, delete mock datastore support. **Domain ports, services, and controllers stay unchanged.**

## 1. Delete this directory

- [ ] Delete entire `telemedicine/datastore/` folder

## 2. Delete backend mock module

- [ ] Delete `telemedicine-backend/shared/java-common-mock-data/`
- [ ] Remove from `settings.gradle`:
  ```gradle
  include 'java-common-mock-data'
  project(':java-common-mock-data').projectDir = file('shared/java-common-mock-data')
  ```

## 3. Per service (e.g. integration-service)

- [ ] Delete `src/main/java/.../infrastructure/mock/` package
- [ ] Delete `src/main/resources/application-mock.yml`
- [ ] Remove from `build.gradle`:
  ```gradle
  implementation project(':java-common-mock-data')
  ```
- [ ] Add `@Profile("postgres")` JPA repository implementing each domain port
- [ ] Set `spring.profiles.active=postgres`

## 4. Verify

- [ ] No references to `MockDatastoreConnection` or `java-common-mock-data`
- [ ] APIs unchanged — only infrastructure adapters swapped

## What you keep

| Keep | Location |
|------|----------|
| Domain models | `.../domain/` |
| Repository ports | `.../domain/*Repository.java` |
| Application services | `.../service/` |
| REST controllers | `.../api/` |
