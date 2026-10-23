-- Docker PostgreSQL Initialization Script for Airport Lost & Found
CREATE ROLE airport_owner NOLOGIN;
CREATE ROLE airport_app WITH LOGIN PASSWORD 'shivam123';

GRANT CONNECT ON DATABASE airport_lost_found TO airport_app;

CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION airport_owner;
GRANT USAGE ON SCHEMA app TO airport_app;

ALTER DEFAULT PRIVILEGES FOR ROLE airport_owner IN SCHEMA app GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO airport_app;
ALTER DEFAULT PRIVILEGES FOR ROLE airport_owner IN SCHEMA app GRANT USAGE, SELECT ON SEQUENCES TO airport_app;
