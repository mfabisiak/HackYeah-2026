# CLAUDE.md – HubMI

Backend platformy Małopolskiego Hubu Innowacji Społecznych (HackYeah 2026, wyzwanie ROPS Kraków).
Opis zadania, moduły i kryteria oceny: [docs/TASK.md](docs/TASK.md); plan prac: [docs/ROADMAP.md](docs/ROADMAP.md), matchmaking: [docs/MATCHMAKING.md](docs/MATCHMAKING.md). **Przeczytaj go przed większą zmianą.**

## Stack

- Kotlin Multiplatform; `:core` (DTO, `@Resource`, wspólne typy; JVM + JS), `:server` (Ktor/Netty, JVM),
  `:web-client` (Kotlin/JS: klient API eksportowany do TypeScripta jako npm `hubmi-client`),
  `web/` (React + TypeScript + Vite + Mantine, prawdziwy DOM dla dostępności). Aplikacja mobilna: poza zakresem.
- **REST API** (JSON, kotlinx.serialization) – bez RPC
- **MongoDB** (`mongodb-driver-kotlin-coroutine`), baza dokumentowa
- **Keycloak** (OIDC/JWT, role realmu `user`, `admin`), **Koin** (DI), **Arrow** (`Either`, `raise`)
- Wersje wyłącznie w `gradle/libs.versions.toml`

## Komendy

```bash
./gradlew :server:test             # testy serwera
./gradlew ktlintCheck              # styl (ktlint_official); ./gradlew ktlintFormat naprawia
./gradlew backendCheck             # to samo co CI: ktlint + testy :server i :core
./gradlew :web-client:jsBrowserProductionLibraryDistribution   # klient Kotlin/JS dla frontendu (po zmianach w :core/:web-client)
(cd web && npm run dev)            # frontend na :5173, proxy do serwera :8080
(cd web && npm run lint && npm run build)
./gradlew :server:run              # serwer lokalnie (wymaga Mongo i Keycloaka)
docker compose up -d --build       # Mongo + Keycloak + serwer
```

Test users: `user`/`user`, `admin`/`admin` (realm [keycloak/hubmi-realm.json](keycloak/hubmi-realm.json)).

## Styl kodu – zasady twarde

1. **Pure Kotlin, funkcyjnie. Zero `var`.** Tylko `val`; zamiast mutacji `copy()`, `map`, `fold`, `buildList`.
   Zero mutowalnych kolekcji w API (`List`, nie `MutableList`). Brak `lateinit`.
2. **Zero rzucania wyjątków w kodzie domenowym.** Nie używamy `throw`, `error()`, `require()`, `check()`, `!!`,
   `TODO()`. Błędy to wartości: `Either<Error, T>` (Arrow). Wyjątki łapiemy **tylko na granicy** z zewnętrznym
   światem (Mongo, JWT, parsowanie) przez `Either.catch { }` (nie połyka `CancellationException`) i od razu
   mapujemy na typowany błąd. Zakaz gołego `try/catch` i `runCatching` poza tą granicą.
3. **Błędy jako `sealed interface`**, osobne per warstwa: `RepositoryError` → `DomainError` → odpowiedź HTTP.
   Mapowanie na status HTTP w jednym miejscu (`respondError`), nigdy w logice domenowej.
4. **Zero `null` w domenie tam, gdzie można uniknąć.** `T?` dozwolone dla „brak dokumentu" z repozytorium
   i opcjonalnych pól; wyciągamy przez `ensureNotNull` / `?:` → błąd domenowy. Nigdy `!!`.
5. **Walidacja przez `either { ensure(...) { Error } }`** z `arrow.core.raise`; wynik łączymy `.bind()`.
6. Dane niemutowalne: `data class` z samymi `val`. Silne typy dla ID (`@JvmInline value class InnovationId(val value: String)`),
   `enum`/`sealed` zamiast stringów-magic.
7. Funkcje małe i czyste; efekty uboczne (IO) tylko w repozytoriach i na brzegu routingu. Preferuj wyrażenia
   (`when`, `if` jako wyrażenie, expression body) nad instrukcjami.
8. Brak `Any`, brak rzutowań (`as`), brak refleksji. `as?` tylko na granicy z biblioteką.
9. `suspend` wszędzie gdzie IO; **nie blokujemy wątków**, żadnego `runBlocking` poza `main`/testami.

## Architektura serwera (`server/src/main/kotlin/io/github/mfabisiak/hubmi/`)

Układ wzorowany na projekcie `schlafzentrale` (`../../BazyDanychProjekt/schlafzentrale`), z REST zamiast kRPC.

```
config/       AppConfig – konfiguracja z env (data class, bez globalnych singletonów)
plugins/      konfiguracja Ktor: Koin, Serialization, Security, Routing, Indexes/Schemas
routes/       cienkie routy per zasób: parsowanie → serwis → odpowiedź
service/      logika domenowa, zwraca Either<DomainError, T>; bez wiedzy o HTTP i Mongo
repository/   dostęp do Mongo; zwraca Either<RepositoryError, T>; bez logiki biznesowej
models/       dokumenty Mongo (data class) + Mappers.kt (model → DTO)
seeding/      dane przykładowe (żadnych prawdziwych danych osobowych!)
di/           moduły Koin
```

W `:core/commonMain` leżą **DTO i żądania/odpowiedzi API** (współdzielone z klientami) – serializowalne,
niemutowalne, bez zależności od Ktora i Mongo.

Przepływ: `route → service → repository`. Warstwa nie woła warstwy nad sobą. Route nie dotyka repozytorium.

### REST

- Trasy jako **Ktor Resources** (`@Resource` w `:core/commonMain`), współdzielone przez serwer i klienta mobile; bez OpenAPI.
- Prefiks `/api`, zasoby w liczbie mnogiej, rzeczowniki: `GET /api/innovations`, `POST /api/ideas`.
- Metody i statusy zgodnie z semantyką: 200/201 (z `Location`), 204, 400 (walidacja), 401, 403, 404, 409 (konflikt), 5xx.
- Błąd zawsze jako `ErrorResponse(code, message)`; `code` to stabilny identyfikator maszynowy.
- Listy paginowane (`?page=&size=`), filtry w query. Odpowiedzi nigdy nie zwracają modeli Mongo – tylko DTO.
- Autoryzacja: `authenticate(KEYCLOAK_AUTH)` + `requireRole("admin")`. Role w `realm_access.roles`.
  Użytkownik wywołujący pochodzi z tokena (`sub`), nigdy z body żądania.

### MongoDB

- Jedna kolekcja per agregat; repozytorium per kolekcja: `class InnovationRepository(database: MongoDatabase)`.
- Modele: `data class` z `@BsonId val id: ObjectId`; ID w DTO jako `String` (hex) opakowany w value class.
- Indeksy (w tym tekstowe dla matchmakingu) i JSON Schema tworzone przy starcie (`plugins/Indexes.kt`, `Schemas.kt`) – idempotentnie.
- Każda metoda repozytorium: `Either.catch { ... }.mapLeft { RepositoryError.DatabaseException(it) }`;
  naruszenie unikalności → `RepositoryError.Conflict`.
- Wielodokumentowe zmiany w transakcji (`MongoRepository.withTransaction`).
- Connection string z env (`MONGO_URI`, `MONGO_DATABASE`).

### DI (Koin)

- Wszystkie zależności przez konstruktor; moduł w `di/`. Bez `inject()` w logice domenowej – tylko w routach/pluginach.
- Testy podmieniają moduły przez `module(extraModules)`.

## Frontend (`web/`) i klient (`:web-client`)

- UI to **TypeScript + React** (Vite, Mantine, React Router). Kontrakt z backendem **nie jest pisany w TS**:
  trasy (`@Resource`) i DTO żyją w `:core`, a `:web-client` opakowuje je w klienta Ktor i eksportuje do TS (`HubApi`).
- **Granica Kotlin/JS ↔ TS:** w `:web-client` eksportujemy tylko typy przyjazne JS (`String`, `Int`, `Double`, `Boolean`,
  `Array`, nullable) – bez `value class`, `Long`, `List`. Metody `suspend` zwracają `Promise`. Błędy nie są wyjątkami:
  każdy wynik to `ApiResult<T>` (`value` albo `error`); w Kotlinie `Either` mapujemy na `ApiResult` w jednym miejscu (`toResult`).
- Nowy endpoint: DTO + `@Resource` w `:core` → implementacja w `:server` → metoda w `HubApi` + typ `...Js` w `:web-client`
  → użycie w React. Po zmianie w Kotlinie przebuduj klienta (komenda wyżej).
- W kodzie TS: `const` zamiast `let`, brak `any`, ESLint z `jsx-a11y` musi przechodzić (`npm run lint`).
- **WCAG 2.1 AA jest wymaganiem** (20% oceny): semantyczny HTML, obsługa klawiatury, widoczny focus, kontrast ≥ 4.5:1,
  komunikaty błędów tekstem (nie samym kolorem), `aria-live` dla wyników, `lang="pl"`, skalowanie do 200%,
  link „Przejdź do treści", `aria-label` dla przycisków-ikon. Preferuj komponenty Mantine, które mają wbudowaną obsługę a11y.
- Zasoby (fonty, ikony) self-hosted; token dostępu tylko w pamięci aplikacji, nie w `localStorage`.
- Żadnych zewnętrznych usług w przeglądarce (CDN, analityka, Web Speech API).

## Konwencje

- Pakiet bazowy: `io.github.mfabisiak.hubmi`. Jeden publiczny typ główny na plik, nazwa pliku = nazwa typu.
- Nazwy w kodzie **po angielsku**; komunikaty błędów dla użytkownika, seed, README i dokumentacja **po polsku**.
- Formatowanie: ktlint (`ktlint_official`, konfiguracja w `.editorconfig`), max 120 znaków, 4 spacje, trailing commas, wildcard-importy tylko `io.ktor.*` i `kotlin.test.*`. Przed commitem: `./gradlew ktlintFormat`.
- Komentarze tylko gdy wyjaśniają *dlaczego*; nie opisują *co* robi kod. Brak martwego kodu i zakomentowanych bloków.
- Sekrety i konfiguracja wyłącznie z env, nigdy w repo (dane z realmu testowego to wyjątek, tylko dev).

## Testy

- `kotlin.test` + `ktor-server-test-host`. Każdy endpoint: scenariusz sukcesu, 401, 403, walidacja.
- Serwisy testujemy z prawdziwym Mongo w kontenerze (Testcontainers) lub z fake'iem repozytorium – nie mockujemy Arrow.
- Matchmaking: testy trafności na zestawie seedowym (znane problemy → oczekiwane innowacje).
- Asercje na `Either`: `assertIs<Either.Right<*>>` / sprawdzanie typu błędu, nie na tekście komunikatu.

## Czego nie robić

- Nie dodawać `var`, `throw`, `!!`, `lateinit`, `runBlocking`, `GlobalScope`.
- Nie zwracać encji Mongo z API ani nie logować danych osobowych / tokenów.
- Nie używać prawdziwych danych osobowych z materiałów ROPS (wymóg wyzwania).
- Nie wysyłać żadnych danych do zewnętrznych serwisów AI – modele (embeddingi, LLM) działają lokalnie.
- Nie omijać Keycloaka własnymi tokenami/hasłami – tożsamość to wyłącznie Keycloak.
- Nie rozbudowywać poza zakres: najpierw matchmaking (obligatoryjny, 10% + największy wpływ na trafność), potem moduły wg punktacji.
