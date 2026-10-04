# CLAUDE.md – HubMI

Backend platformy Małopolskiego Hubu Innowacji Społecznych (HackYeah 2026, wyzwanie ROPS Kraków).
Opis zadania, moduły i kryteria oceny: [docs/TASK.md](docs/TASK.md); plan prac: [docs/ROADMAP.md](docs/ROADMAP.md), matchmaking: [docs/MATCHMAKING.md](docs/MATCHMAKING.md), backlog: [docs/BACKEND_TICKETS.md](docs/BACKEND_TICKETS.md), [docs/FRONTEND_TICKETS.md](docs/FRONTEND_TICKETS.md). **Przeczytaj go przed większą zmianą.**

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
docker compose up -d --build       # Mongo + Keycloak + serwer + frontend (:3000)
```

Test users: `user`/`user`, `admin`/`admin` (realm [keycloak/hubmi-realm.json](keycloak/hubmi-realm.json)).

## Styl kodu – zasady twarde

1. **Pure Kotlin, funkcyjnie. Zero `var`.** Tylko `val`; zamiast mutacji `copy()`, `map`, `fold`, `buildList`.
   Zero mutowalnych kolekcji w API (`List`, nie `MutableList`). Brak `lateinit`.
2. **Zero rzucania wyjątków w kodzie domenowym.** Nie używamy `throw`, `error()`, `require()`, `check()`, `!!`,
   `TODO()`. Błędy to wartości: `Either<Error, T>` (Arrow). Wyjątki łapiemy **tylko na granicy** z zewnętrznym
   światem (Mongo, JWT, parsowanie) przez `Either.catch { }` (nie połyka `CancellationException`) i od razu
   mapujemy na typowany błąd. Zakaz gołego `try/catch` i `runCatching` poza tą granicą. Jedyny wyjątek: ostatnia deska
   ratunku w `StatusPages` (`500` z logiem) – nie służy do obsługi błędów domenowych.
   Konfiguracja z env ładuje się jako `Either<ConfigError, AppConfig>`; `main` kończy proces przy błędzie.
3. **Błędy jako `sealed interface`**, osobne per warstwa: `RepositoryError` → `DomainError` → odpowiedź HTTP.
   Mapowanie na status HTTP w jednym miejscu (`respondError`), nigdy w logice domenowej.
4. **Zero `null` w domenie tam, gdzie można uniknąć.** `T?` dozwolone dla „brak dokumentu" z repozytorium
   i opcjonalnych pól; wyciągamy przez `ensureNotNull` / `?:` → błąd domenowy. Nigdy `!!`.
5. **Walidacja przez `either { ensure(...) { Error } }`** z `arrow.core.raise`; wynik łączymy `.bind()`.
   Dane z żądania parsujemy **na początku requestu** (w routingu) do value classes i `*Draft` z pakietu domeny
   (`zipOrAccumulate` zbiera wszystkie błędy pól); serwis i repozytorium dostają już tylko typy domenowe.
6. Dane niemutowalne: `data class` z samymi `val`. Silne typy dla ID w domenie (`@JvmInline value class InnovationId(val value: String)`),
   `enum`/`sealed` zamiast stringów-magic. **Wyjątek: kontrakt API w `:core`** – ID to `String`, czas to ISO-8601 `String`,
   żeby kontrakt dał się wyeksportować do JS.
7. Funkcje małe i czyste; efekty uboczne (IO) tylko w repozytoriach i na brzegu routingu. Preferuj wyrażenia
   (`when`, `if` jako wyrażenie, expression body) nad instrukcjami.
8. Brak `Any`, brak rzutowań (`as`), brak refleksji. `as?` tylko na granicy z biblioteką.
9. `suspend` wszędzie gdzie IO; **nie blokujemy wątków**, żadnego `runBlocking` poza `main`/testami. Jedyny wyjątek to
   jednorazowa inicjalizacja bazy przy starcie w `Application.module` (indeksy, seed): serwer nie przyjmuje żądań, dopóki
   się nie skończy, bo unikalne indeksy są warunkiem poprawności upsertów.

## Idiomatyczny Kotlin – typy zamiast stringów

Typ ma wyrażać znaczenie. Zanim napiszesz literał `"..."`, zapytaj: czy to zamknięty zbiór wartości? Jeśli tak – `enum`.

1. **Zamknięty zbiór = `enum class` (lub `sealed interface`), nigdy `String`.** Dotyczy: ról (`Role`), kodów błędów
   (`ErrorCode`, `FieldErrorCode`, `Role`), statusów, etapów, obszarów, typów. Porównania `"admin" in roles`, `code = "not_found"`,
   `status == "OPEN"` są zakazane. Wartości zewnętrzne (nazwa roli w Keycloaku) mapujemy na enum **na granicy**
   (`Role.fromKeycloakName`); nieznane wartości odrzucamy tam, nie w domenie.
2. **Wariant z danymi = `sealed interface`, nie enum + osobne pola.** Gdy wartość niesie parametry (limit, zakres),
   robimy `data class`, a warianty bez danych to `data object`: `FieldErrorCode.MinValue(min)`, `FieldErrorCode.Range(min, max)`,
   `FieldErrorCode.Blank`. Nie wciskamy parametrów w komunikat tekstowy ani w osobne `String`-pola.
   Enum zostaje tylko dla wartości bez danych (`ErrorCode`, `Role`). W `:core` każdy wariant ma `@Serializable` i `@SerialName`.
3. **Enum w kontrakcie API** leci jako nazwa wpisu (`NOT_FOUND`) – tak jak `SocialArea`. Nie dorabiamy równoległych
   stringów `snake_case`; frontend dostaje `enum.name`.
4. **Parametry funkcji to typy domenowe**, nie `String`/`Int` „z umowy”: `requireRole(Role.ADMIN)`, nie `requireRole("admin")`.
5. **Stałe zamiast powtarzanych literałów.** Nazwa używana w ≥ 2 miejscach (kolekcja, claim JWT, klucz konfiguracji)
   to `const val` w jednym miejscu. Literał tekstowy w kodzie dopuszczamy tylko dla komunikatów użytkownika.
6. **`when` na enumie/sealed bez `else`** – kompilator ma wymusić obsłużenie nowego przypadku.
7. **Preferuj `value class` i `data class` nad `Pair`/`Triple`/`Map<String, Any>`.** Wynik z więcej niż jedną wartością
   dostaje nazwany typ.
8. **Kolekcje: `map`/`filter`/`mapNotNull`/`associateBy`/`groupBy`/`fold`**, nie pętle z akumulatorem. `buildList` zamiast
   `MutableList` w zmiennej. Sekwencje (`asSequence()`) dla łańcuchów na dużych listach.
9. **Referencje do funkcji i właściwości** (`Role::fromKeycloakName`, `SampleItem::slug`) zamiast lambd-wrapperów i stringów.
10. **Scope functions oszczędnie:** `let` do nullable (`?.let`), `apply`/`also` tylko przy konfiguracji obiektu; zero
   zagnieżdżonych scope functions. Kolejność: guard clause / `ensure` na górze, brak głębokiego zagnieżdżania.
11. **Nullable:** `?.`, `?:`, `ensureNotNull`; domyślne wartości parametrów zamiast przeciążeń; named arguments przy
    ≥ 3 parametrach lub parametrach tego samego typu (`FieldError(field = ..., code = ..., message = ...)`).
12. **Nie używaj `Document`/`Map<String, Any?>`/`JsonObject` jako modelu domeny.** Dane mają mieć klasę.
13. **Nieużywany kod usuwamy** (nieużywane importy, typy, parametry, aliasy `typealias` „na zapas”, duplikaty funkcji
    o tej samej roli). Nie dodajemy abstrakcji i pól „na przyszłość” – także indeksów i kolekcji bez modelu.
14. Własności wyliczane (`val isAdmin get() = ...`) tylko gdy trywialne; w innym wypadku zwykła funkcja.

## Architektura serwera (`server/src/main/kotlin/io/github/mfabisiak/hubmi/`)

Układ wzorowany na projekcie `schlafzentrale` (`../../BazyDanychProjekt/schlafzentrale`), z REST zamiast kRPC.

```
innovations/  pakiety domenowe: wszystko o jednym agregacie w jednym pakiecie
challenges/     (`Innovation*`, `Challenge*`, `Material*`, `Sample*`, `Feedback*`/`TestRequest*` w `tester/`):
materials/      `XxxId`/`XxxDraft`/value classes (`parse(request)` → `Either`; bez Ktora i Mongo), `XxxItem` (dokument Mongo
samples/        + `const val ..._COLLECTION` i `MongoDatabase.xxx` w repozytorium), `XxxMappers` (model → DTO),
                `XxxRepository` (Either<RepositoryError, T>; bez logiki), `XxxService` (Either<DomainError, T>; bez HTTP i Mongo),
                `XxxRoutes` (cienkie routy: parsowanie → serwis → odpowiedź)
matching/     dopasowanie potrzeb do innowacji (`POST /api/matches`): silniki (BM25, oczyszczanie danych osobowych, indeks
                w pamięci – czyste, bez IO poza `InnovationIndex`), `MatchService`, `NeedItem`/`NeedRepository`
auth/         Keycloak/JWT: `configureSecurity`, `requireRole`, `CurrentUser`, `/api/me`, `/api/admin`
health/       `/`, `/api/health`, `/api/health/ready`
common/       współdzielone: `DomainError`, `RepositoryError`, paginacja, parsowanie i value classes używane przez wiele
                domen (`Title`, `Description`, `HttpUrl`, `SearchQuery`)
common/mongo/ infrastruktura Mongo: `MongoRepository` (klient), `SoftDeleteRepository`, `catching`/`mongoCatch`, filtry
common/http/  mapowanie `DomainError` → odpowiedź HTTP (`respondError`, `respondEither`…)
config/       AppConfig – konfiguracja z env (data class, bez globalnych singletonów)
plugins/      konfiguracja Ktor: Koin, Serialization, Indexes
seeding/      dane przykładowe (żadnych prawdziwych danych osobowych!); pliki w `resources/seed/`, format i skrypt
                odświeżający bibliotekę ROPS (`scripts/scrape_rops.py`): `resources/seed/README.md`
di/           moduły Koin
```

Podział jest **domenowy, nie warstwowy**: nowa funkcjonalność = nowy pakiet z własnym `Id`/`Draft`/`Item`/`Repository`/
`Service`/`Routes`. Domeny nie zależą od siebie wzajemnie (wyjątek: `matching` korzysta z `innovations`); to, co potrzebuje więcej niż jedna, trafia do `common`.

W `:core/commonMain` leżą **DTO i żądania/odpowiedzi API** (współdzielone z klientami) – serializowalne,
niemutowalne, bez zależności od Ktora i Mongo.

Przepływ: `route (parsowanie do typów domenowych) → service → repository`. Warstwa nie woła warstwy nad sobą. Route nie dotyka repozytorium.

### REST

- Kontrakt API: [docs/API.md](docs/API.md) (zasoby i DTO w `:core/.../api`).
- Trasy jako **Ktor Resources** (`@Resource` w `:core/commonMain`), współdzielone przez serwer i klienta mobile; bez OpenAPI.
- Prefiks `/api`, zasoby w liczbie mnogiej, rzeczowniki: `GET /api/innovations`, `POST /api/ideas`.
- Metody i statusy zgodnie z semantyką: 200/201 (z `Location`), 204, 400 (walidacja), 401, 403, 404, 409 (konflikt), 5xx.
- Błąd zawsze jako `ErrorResponse(code, message)`; `code` to enum `ErrorCode` z `:core` (a `FieldError.code` to sealed `FieldErrorCode`) –
  nigdy wolny string. Nowy rodzaj błędu = nowy wpis w enumie.
- Listy paginowane (`?page=&size=`), filtry w query. Odpowiedzi nigdy nie zwracają modeli Mongo – tylko DTO.
- Autoryzacja: `authenticate(KEYCLOAK_AUTH)` + `requireRole(Role.ADMIN)`. Role to enum `Role` z `:core`, mapowany z
  `realm_access.roles` na granicy (nieznane role Keycloaka są pomijane).
  Użytkownik wywołujący pochodzi z tokena (`sub`), nigdy z body żądania.

### MongoDB

- Jedna kolekcja per agregat; repozytorium per kolekcja: `class InnovationRepository(database: MongoDatabase)`.
- Modele: `data class` z `@BsonId val id: ObjectId`; ID w DTO jako `String` (hex) opakowany w value class.
- Indeksy tworzone przy starcie (`plugins/MongoIndexes.kt`) – idempotentnie. **Bez walidatorów JSON Schema w Mongo**: typy
  pilnują modele `@Serializable`, a dane wejściowe – value classes i `*Draft` w pakietach domen.
- Kolekcje z soft-delete (`archived`) dziedziczą `SoftDeleteRepository` (paginacja, update, archiwizacja); repozytorium
  dodaje tylko filtry i pola swojej kolekcji.
- Każda metoda repozytorium: `Either.catch { ... }.mapLeft { RepositoryError.DatabaseException(it) }`;
  naruszenie unikalności → `RepositoryError.Conflict`.
- **Bez transakcji wielodokumentowych.** Modelujemy dane tak, by agregat mieścił się w jednym dokumencie, a każda
  mutacja była atomową operacją na jednym dokumencie: `findOneAndUpdate`, `$inc`, `$push`, `$set`, upsert po kluczu
  unikalnym. Warunki w filtrze (np. `{_id, status: oczekiwany}`) dają optymistyczną współbieżność i maszyny stanów.
  Dane pochodne (agregaty, `lastMessageAt`) są idempotentne i samonaprawiające (przeliczane z danych źródłowych), a nie
  utrzymywane „na styk" w dwóch dokumentach naraz.
- **Kolekcje są typowane: `MongoCollection<Model>`, nigdy `MongoCollection<Document>`.** Binding nazwa → typ jest w jednym
  miejscu (w pakiecie domeny, obok repozytorium: `const val ..._COLLECTION` + `val MongoDatabase.samples`). Repozytoria,
  indeksy, schematy, seedery i testy biorą kolekcję stamtąd, nie wołają `getCollection("nazwa")`.
- **Kodeki: bson-kotlinx.** Modele w Mongo to `@Serializable data class`; `_id` jako
  `@SerialName("_id") @Contextual val id: ObjectId`. Enumy, `value class` i `Instant` koduje kodek, nie ręczne mapowanie.
  Nowy typ niewspierany przez bson-kotlinx = własny `KSerializer`/`Codec` zarejestrowany w jednym miejscu, nie
  konwersja do `Document` w repozytorium.
- **Pola przez referencje do właściwości, nie przez stringi:** `Filters.eq(SampleItem::slug, slug)`,
  `Sorts.descending(SampleItem::id)`, `Indexes.ascending(SampleItem::slug)`, `Updates.set(SampleItem::name, v)`
  z `com.mongodb.kotlin.client.model.*` (`mongodb-driver-kotlin-extensions`); nazwę pola w ręcznym BSON-ie
  bierzemy z `SampleItem::slug.path()`. Zakaz `Filters.eq("slug", ...)`, `"_id"`, `Document("x" to ...)` dla danych.
- Stałe i buildery z drivera zamiast literałów (`Indexes.text(...)`, `Sorts.descending(...)`).
  `Document` dopuszczalny wyłącznie dla poleceń bazy (`ping`).
- Indeksy i seed powstają razem z modelem i kolekcją, której dotyczą – nie wyprzedzamy kolejnych ticketów.
- Seed jest idempotentny przez upsert po `_id` wyliczonym z klucza naturalnego z pliku seed (`slug` → hash), bez pól
  technicznych w modelu domeny. `$setOnInsert`: ponowny seed nie nadpisuje edycji ani archiwizacji zrobionych przez admina.
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
- Nowy endpoint: DTO + `@Resource` w `:core` → implementacja w `:server` → metoda w module `...Api` + typ `...Js` w `:web-client`
  → użycie w React. Po zmianie w Kotlinie przebuduj klienta (komenda wyżej).
- **Układ `:web-client`** (`web/`, podpakiety per moduł: `innovations`, `knowledge`, `matching`, `ideas`, `messaging`, `admin`):
  `HubApi` trzyma moduły (`hubApi.innovations.list(...)`). W podpakiecie leżą dwa pliki: `XxxApi(s).kt` (klasy `...Api`)
  oraz `XxxTypes.kt` (wszystkie typy `...Js` modułu razem z mapowaniem DTO ↔ `...Js`). Wspólne helpery HTTP (`Fetch.kt`:
  `fetch`, `send`, `sendForUnit`), parsowanie enumów (`Arguments.kt`) i typy wspólne (`JsTypes.kt`: `ApiResult`,
  `ApiErrorJs`, `PageJs`, `EmptyJs`) są w `web/`. Nie mnożymy plików: jeden plik na typ obowiązuje w `:server`, a tu typy
  `...Js` to cienkie kontenery danych grupowane per moduł.
- Enumy przechodzą granicę JS jako nazwy (`"AGING"`), a parsuje je `enumOf`/`enumOrNull`/`enumsOf`; nieznana nazwa to
  `ApiResult` z błędem `INVALID_ARGUMENT` (status `0`), nie wyjątek. `Map` z DTO zamieniamy na tablicę par (`AnswerJs`),
  odpowiedź `204` to `ApiResult<EmptyJs>`. Kody błędów klienta (`NETWORK_ERROR`, `INVALID_RESPONSE`, `HTTP_<status>`) mają
  tę samą konwencję `UPPER_SNAKE` co `ErrorCode`.
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
- Matchmaking: testy trafności na zestawie seedowym (znane problemy → oczekiwane innowacje): `MatchingQualityTest` liczy
  hit@3 i MRR i pilnuje celów; `WRITE_MATCHING_BASELINE=true ./gradlew :server:test` odświeża `docs/matching-baseline.md`.
- Asercje na `Either`: `assertIs<Either.Right<*>>` / sprawdzanie typu błędu, nie na tekście komunikatu.

## Czego nie robić

- Nie dodawać `var`, `throw`, `!!`, `lateinit`, `runBlocking`, `GlobalScope`.
- Nie używać stringów tam, gdzie powinien być enum (role, kody błędów, statusy), ani `MongoCollection<Document>` dla danych.
- Nie dodawać kodu „na zapas” (indeksy/kolekcje/pola bez modelu i użycia).
- Nie zwracać encji Mongo z API ani nie logować danych osobowych / tokenów.
- Nie używać prawdziwych danych osobowych z materiałów ROPS (wymóg wyzwania).
- Nie wysyłać żadnych danych do zewnętrznych serwisów AI – modele (embeddingi, LLM) działają lokalnie.
- Nie omijać Keycloaka własnymi tokenami/hasłami – tożsamość to wyłącznie Keycloak.
- Nie rozbudowywać poza zakres: najpierw matchmaking (obligatoryjny, 10% + największy wpływ na trafność), potem moduły wg punktacji.
