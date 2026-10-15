-- Schema-per-service: one database + owner role per bounded context.
-- Flyway migrations in each service target its own database.

DO $$
DECLARE
  svc TEXT;
  services TEXT[] := ARRAY[
    'waiting_room', 'identity', 'video', 'telemetry', 'clinical_docs',
    'notification', 'integration', 'survey', 'reporting', 'monitoring',
    'config', 'audit', 'scheduler', 'physician'
  ];
BEGIN
  FOREACH svc IN ARRAY services
  LOOP
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = svc) THEN
      EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', svc, 'change-me-locally');
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = svc) THEN
      EXECUTE format('CREATE DATABASE %I OWNER %I', svc, svc);
    END IF;
    EXECUTE format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', svc, svc);
  END LOOP;
END
$$;
