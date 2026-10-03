#!/bin/sh
# Local dev: builds the Kotlin/JS API client (:web-client) that the React app in web/ consumes as "hubmi-client".
cd "$(dirname "$0")/.." || exit 1
exec ./gradlew :web-client:jsBrowserProductionLibraryDistribution
