-- Idempotently create the physician service database + owner role.
SELECT 'CREATE ROLE physician LOGIN PASSWORD ''change-me-locally'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'physician')
\gexec

SELECT 'CREATE DATABASE physician OWNER physician'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'physician')
\gexec

GRANT ALL PRIVILEGES ON DATABASE physician TO physician;
