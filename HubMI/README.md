# HubMI – Małopolski Hub Innowacji Społecznych

Prototyp platformy dla wyzwania ROPS Kraków (HackYeah 2026). Opis zadania: [docs/TASK.md](docs/TASK.md),
plan prac: [docs/ROADMAP.md](docs/ROADMAP.md), matchmaking: [docs/MATCHMAKING.md](docs/MATCHMAKING.md),
zasady kodu: [CLAUDE.md](CLAUDE.md).

## Struktura

| Moduł                  | Co to jest                                                                              |
|:-----------------------|:----------------------------------------------------------------------------------------|
| [core](core)           | Kotlin Multiplatform (JVM + JS): kontrakt API – DTO i trasy Ktor Resources              |
| [server](server)       | Backend: Ktor + Koin + MongoDB, uwierzytelnianie przez Keycloak                         |
| [web-client](web-client) | Kotlin/JS: klient API używający kontraktu z `core`, eksportowany do TypeScripta (npm: `hubmi-client`) |
| [web](web)             | Frontend: React + TypeScript (Vite), komponenty Mantine, dostępność WCAG 2.1 AA         |
| [keycloak](keycloak)   | Import realmu deweloperskiego                                                           |

Kontrakt jest kodem Kotlina: `core` → serwer implementuje trasy, `web-client` je wywołuje, `web` używa wygenerowanych
typów TypeScript. Zmiana w `core` psuje kompilację po obu stronach.

## Uruchomienie lokalne

Wymagane: JDK 21, Node 22+ (zalecane 24), Docker.

```bash
docker compose up -d --build          # Mongo + Keycloak + serwer (http://localhost:8080)
./gradlew :web-client:jsBrowserProductionLibraryDistribution   # buduje klienta Kotlin/JS dla frontendu
cd web && npm install && npm run dev  # http://localhost:5173 (proxy /api i /health -> :8080)
```

Po zmianie kodu w `core` lub `web-client` zbuduj klienta ponownie (druga komenda, albo `npm run client` w `web/`).

- Serwer: http://localhost:8080 (`/`, `/health` publiczne; `/api/me` wymaga tokena; `/api/admin` wymaga roli `admin`)
- Keycloak: http://localhost:8081 (konsola: `admin` / `admin`), realm `hubmi`, publiczny klient `hubmi-app`
- Użytkownicy testowi ([keycloak/hubmi-realm.json](keycloak/hubmi-realm.json)): `user`/`user`, `admin`/`admin`
- Mongo: replica set jednowęzłowy na porcie `27017` (gdy zajęty: `MONGO_PORT=27018 docker compose up -d`)

Konfiguracja serwera przez zmienne środowiskowe: `KEYCLOAK_ISSUER` (musi równać się `iss` tokena),
`KEYCLOAK_JWKS_URL`, `MONGO_URI`, `MONGO_DATABASE`, `PORT`. Frontend: `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`,
`VITE_KEYCLOAK_CLIENT_ID`, a w dev `HUBMI_BACKEND` (adres serwera dla proxy Vite).

Realm Keycloaka importuje się tylko przy pierwszym starcie. Po zmianie [keycloak/hubmi-realm.json](keycloak/hubmi-realm.json)
usuń wolumeny (`docker compose down -v`) albo zaktualizuj klienta w konsoli.

Token z linii poleceń:

```bash
TOKEN=$(curl -s -d client_id=hubmi-app -d grant_type=password -d username=admin -d password=admin \
  http://localhost:8081/realms/hubmi/protocol/openid-connect/token | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/me
```

## Testy i styl

```bash
./gradlew backendCheck                 # ktlint + testy :server i :core (to samo robi CI)
./gradlew ktlintFormat                 # formatowanie Kotlina
cd web && npm run lint && npm run build  # ESLint (z regułami a11y) + typecheck + build
```
