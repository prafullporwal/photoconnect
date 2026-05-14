#!/bin/bash
# =============================================================================
# Postgres entrypoint script
# =============================================================================
# The official postgres image only creates ONE database (POSTGRES_DB) on first
# boot. Our setup needs two — one per service that uses Postgres — so we read
# a comma-separated list from POSTGRES_MULTIPLE_DATABASES and create them all.
#
# This script is mounted into /docker-entrypoint-initdb.d/ and runs ONCE on
# the very first container startup (when the data volume is empty).
# =============================================================================
set -e
set -u

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Creating multiple databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
        echo "  -> creating database '$db'"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            CREATE DATABASE "$db";
            GRANT ALL PRIVILEGES ON DATABASE "$db" TO "$POSTGRES_USER";
EOSQL
    done
    echo "Multiple databases created."
fi
