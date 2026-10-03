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

Wszystko jednym poleceniem (Mongo, Keycloak, serwer i frontend za nginx):

```bash
docker compose up -d --build          # frontend: http://localhost:3000, serwer: http://localhost:8080
```

Praca nad frontendem z hot reloadem (backend nadal w Dockerze):

```bash
docker compose up -d --build server   # Mongo + Keycloak + serwer (http://localhost:8080)
./gradlew :web-client:jsBrowserProductionLibraryDistribution   # buduje klienta Kotlin/JS dla frontendu
cd web && npm install && npm run dev  # http://localhost:5173 (proxy /api i /health -> :8080)
```

Po zmianie kodu w `core` lub `web-client` zbuduj klienta ponownie (komenda z `./gradlew`, albo `npm run client` w `web/`).

- Frontend (nginx w Dockerze): http://localhost:3000 (`WEB_PORT` zmienia port; Keycloak zezwala na originy 3000, 4173 i 5173)
- Serwer: http://localhost:8080 (`/`, `/health` publiczne; `/api/me` wymaga tokena; `/api/admin` wymaga roli `admin`)
- Keycloak: http://localhost:8081 (konsola: `admin` / `admin`), realm `hubmi`, publiczny klient `hubmi-app`
- Użytkownicy testowi ([keycloak/hubmi-realm.json](keycloak/hubmi-realm.json)): `user`/`user`, `admin`/`admin`
- Mongo: replica set jednowęzłowy na porcie `27017` (gdy zajęty: `MONGO_PORT=27018 docker compose up -d`)

Konfiguracja serwera przez zmienne środowiskowe: `KEYCLOAK_ISSUER` (musi równać się `iss` tokena),
`KEYCLOAK_JWKS_URL`, `MONGO_URI`, `MONGO_DATABASE`, `PORT`, `MATCHING_MODE`, `OLLAMA_URL`, `EMBEDDING_MODEL`. Frontend: `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`,
`VITE_KEYCLOAK_CLIENT_ID`, a w dev `HUBMI_BACKEND` (adres serwera dla proxy Vite).

### Matchmaking: Ollama (embeddingi)

Dopasowanie problemu do innowacji (`POST /api/matches`) działa w trybie `MATCHING_MODE=hybrid` (domyślny): tekst (BM25) +
znaczenie (embeddingi `bge-m3`). Embeddingi liczy **lokalna Ollama** – nic nie opuszcza maszyny. Bez Ollamy serwer
działa dalej na samym tekście (`MATCHING_MODE=keyword` wymusza ten tryb).

**Wariant A: Ollama na hoście** (zalecany na Macu: używa GPU przez Metal):

```bash
brew install ollama && brew services start ollama   # Windows: winget install Ollama.Ollama
ollama pull bge-m3                                  # ~1,2 GB, jednorazowo
docker compose up -d --build
```

Serwer w Dockerze łączy się z hostem przez `host.docker.internal:11434` (ustawione w `docker-compose.yml`); serwer
uruchomiony lokalnie (`./gradlew :server:run`) używa `http://localhost:11434`.

**Wariant B: Ollama w Dockerze** (bez instalacji na hoście; tylko CPU, co przy embeddingach wystarcza):

```bash
OLLAMA_URL=http://ollama:11434 docker compose --profile ollama up -d --build
```

Profil `ollama` uruchamia kontener `ollama` oraz jednorazowy `ollama-pull`, który pobiera model do wolumenu
`ollama-data` (~1,2 GB, tylko za pierwszym razem). Do czasu pobrania serwer odpowiada na samych słowach kluczowych,
a potem sam przechodzi na tryb hybrydowy (do 30 s). Postęp: `docker compose logs -f ollama-pull`.

Zmienne: `MATCHING_MODE` (`keyword` | `hybrid`), `OLLAMA_URL`, `EMBEDDING_MODEL`. Szczegóły i pomiary:
[docs/MATCHMAKING.md](docs/MATCHMAKING.md).

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
