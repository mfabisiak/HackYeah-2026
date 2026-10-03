@echo off
rem Local dev: builds the Kotlin/JS API client (:web-client) that the React app in web/ consumes as "hubmi-client".
cd /d "%~dp0.."
call gradlew.bat :web-client:jsBrowserProductionLibraryDistribution
exit /b %ERRORLEVEL%
