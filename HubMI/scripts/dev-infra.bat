@echo off
rem Local dev: Postgres, Mongo and Keycloak in Docker; the server and web run outside of it.
cd /d "%~dp0.."
docker compose stop server web
docker compose up -d --wait postgres mongo keycloak
exit /b %ERRORLEVEL%
