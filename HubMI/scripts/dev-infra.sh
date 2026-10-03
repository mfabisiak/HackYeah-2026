#!/bin/sh
# Local dev: Postgres, Mongo and Keycloak in Docker; the server and web run outside of it.
cd "$(dirname "$0")/.." || exit 1
docker compose stop server web
docker compose up -d --wait postgres mongo keycloak
