@echo off
rem Local dev: Vite on :5173 (proxies to the server on :8080). Installs dependencies when the lockfile changed.
cd /d "%~dp0..\web"
powershell -NoProfile -Command "if (-not (Test-Path node_modules/.package-lock.json) -or (Get-Item package-lock.json).LastWriteTime -gt (Get-Item node_modules/.package-lock.json).LastWriteTime) { exit 1 }"
if errorlevel 1 (
    call npm ci
    if errorlevel 1 exit /b 1
)
call npm run dev
