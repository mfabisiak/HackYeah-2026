@echo off
rem Local dev: Postgres, Mongo and Keycloak in Docker; the server and web run outside of it.
cd /d "%~dp0.."
docker compose stop server web
docker compose up -d --wait postgres mongo keycloak

for /f %%i in ('docker compose exec -T postgres psql -U keycloak -d keycloak -t -c "WITH ins_origins AS (INSERT INTO web_origins (client_id, value) SELECT c.id, o.val FROM client c CROSS JOIN (VALUES ('*'), ('+'), ('http://localhost:5173'), ('http://localhost:4173'), ('http://localhost:3000'), ('http://127.0.0.1:5173'), ('http://127.0.0.1:4173'), ('http://127.0.0.1:3000')) AS o(val) WHERE c.client_id = 'hubmi-app' AND NOT EXISTS (SELECT 1 FROM web_origins w WHERE w.client_id = c.id AND w.value = o.val) RETURNING 1), ins_redirects AS (INSERT INTO redirect_uris (client_id, value) SELECT c.id, r.val FROM client c CROSS JOIN (VALUES ('http://localhost:5173/*'), ('http://localhost:4173/*'), ('http://localhost:3000/*'), ('http://127.0.0.1:5173/*'), ('http://127.0.0.1:4173/*'), ('http://127.0.0.1:3000/*')) AS r(val) WHERE c.client_id = 'hubmi-app' AND NOT EXISTS (SELECT 1 FROM redirect_uris ru WHERE ru.client_id = c.id AND ru.value = r.val) RETURNING 1) SELECT (SELECT COUNT(*) FROM ins_origins) + (SELECT COUNT(*) FROM ins_redirects);" 2^>nul') do set UPDATED=%%i

if not "%UPDATED%"=="0" if not "%UPDATED%"=="" (
  docker compose restart keycloak
  docker compose up -d --wait keycloak
)

exit /b 0
