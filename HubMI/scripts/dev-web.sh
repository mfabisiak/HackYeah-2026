#!/bin/sh
# Local dev: Vite on :5173 (proxies to the server on :8080). Installs dependencies when the lockfile changed.
cd "$(dirname "$0")/../web" || exit 1
if [ package-lock.json -nt node_modules/.package-lock.json ]; then
    npm ci || exit 1
fi
exec npm run dev
