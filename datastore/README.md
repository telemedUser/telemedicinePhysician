# Telemedicine datastore (optional — safe to delete)

**Separate directory** from application code. Holds JSON seed files that act as a stand-in database during local development.

```
telemedicine/
├── datastore/              ← you are here (like a database server + data files)
│   ├── seeds/              ← table data as JSON collections
│   │   └── {service}/
│   │       └── {collection}.json
│   ├── README.md
│   └── DELETION.md
├── telemedicine-backend/   ← services connect via MockDatastoreConnection
├── telemedicine-frontend/  ← never reads this directory
└── telemedicine-platform/
```

## How backend services connect

Services do **not** read JSON files directly. They open a **mock datastore connection** (same pattern as JDBC):

```
Service → Repository port → MockDatastoreConnection → JSON seeds in datastore/seeds/
                         ↘ JpaRepository (later)   → PostgreSQL
```

Configuration (`application-mock.yml`):

```yaml
telemedicine:
  datastore:
    root: file:../datastore/seeds   # or TELEMEDICINE_DATASTORE_ROOT env var
    writable: true
```

## Collections (like database tables)

| Service | Collection file | Primary key field |
|---------|-----------------|-------------------|
| integration-service | `appointments.json` | `id` |
| integration-service | `mpi-patients.json` | `appointmentId` |

## ID conventions

| Prefix | Meaning | Example |
|--------|---------|---------|
| `apt-*` | Appointment | `apt-demo` |
| `pat-*` | Patient | `pat-001` |

Use **`apt-demo`** for end-to-end journey testing.

## Writable overlay

Inserts/updates go to an **in-memory overlay** only — files in `seeds/` are never modified.

Reset (mock profile, per service):

```http
POST /actuator/mock/reset
```

## Switching to PostgreSQL

1. Implement `@Profile("postgres")` JPA repositories for each domain port.
2. Follow [DELETION.md](./DELETION.md).
3. Set `spring.profiles.active=postgres`.

See [mock-datastore-plan.md](../telemedicine-platform/docs/mock-datastore-plan.md).
