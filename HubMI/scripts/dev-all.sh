#!/bin/sh
# Local dev, everything in one go: Docker infra -> Kotlin/JS client -> server + Vite, both until stopped.
dir="$(dirname "$0")"
cd "$dir/.." || exit 1
"$dir/dev-infra.sh" || exit 1
"$dir/dev-client.sh" || exit 1
SEED=true ./gradlew :server:run &
server=$!
"$dir/dev-web.sh" &
web=$!
# stopping the run configuration (SIGTERM) must take both processes down
trap 'kill "$server" "$web" 2>/dev/null' EXIT INT TERM
wait
