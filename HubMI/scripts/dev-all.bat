@echo off
rem Local dev, everything in one go: Docker infra -> Kotlin/JS client -> server (background) + Vite (foreground).
cd /d "%~dp0.."
call scripts\dev-infra.bat
if errorlevel 1 exit /b 1
call scripts\dev-client.bat
if errorlevel 1 exit /b 1
set SEED=true
start "hubmi-server" /b cmd /c gradlew.bat :server:run
call scripts\dev-web.bat
