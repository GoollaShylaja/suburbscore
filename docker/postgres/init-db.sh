#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
  -- per-service users
  CREATE USER "$USER_DB_USERNAME" WITH PASSWORD '$USER_DB_PASSWORD';
  CREATE USER "$SUBURB_DB_USERNAME" WITH PASSWORD '$SUBURB_DB_PASSWORD';

  -- per-service databases, each owned by its own user
  CREATE DATABASE suburbscore_users   OWNER "$USER_DB_USERNAME";
  CREATE DATABASE suburbscore_suburbs OWNER "$SUBURB_DB_USERNAME";

  -- restrict access so other users cannot connect
  REVOKE ALL ON DATABASE suburbscore_users   FROM PUBLIC;
  REVOKE ALL ON DATABASE suburbscore_suburbs FROM PUBLIC;

  GRANT CONNECT ON DATABASE suburbscore_users   TO "$USER_DB_USERNAME";
  GRANT CONNECT ON DATABASE suburbscore_suburbs TO "$SUBURB_DB_USERNAME";
EOSQL
