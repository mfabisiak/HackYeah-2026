# Roadmapa HubMI

Plan prac (backend + web) na podstawie [TASK.md](TASK.md). Zasady kodu: [CLAUDE.md](../CLAUDE.md).

## Punktacja → priorytety

Stopień spełnienia wyzwania to 40%: **Matchmaking 10%, każdy kolejny moduł +5%**. Siedem modułów daje więc maksimum,
a każdy z sześciu dodatkowych jest wart tyle samo – robimy je **od najtańszego** i płytko-ale-działająco,
zamiast jednego perfekcyjnego. Pozostałe 60% to wdrażalność (20%), dostępność WCAG (20%), UI (10%) i materiały (10%).

| Priorytet | Moduł                          | Koszt | Dlaczego                                                              |
|:---------:|:-------------------------------|:-----:|:----------------------------------------------------------------------|
|     1     | 1. Matchmaking (obligatoryjny) |  duży | rdzeń oceny, „trafność dopasowania" jest osobnym kryterium            |
|     2     | 2. Zasobnik wiedzy             | mały  | CRUD + lista; daje dane dla matchmakingu                              |
|     3     | 3. Kreator pomysłów (fiszka)   | mały  | prosty formularz, wysoka widoczność w demo                            |
|     4     | 6. Panel administratora        | mały  | moderacja + trendy z potrzeb; spina moduły 1–3                        |
|     5     | 4. Tester innowacji            | mały  | zgłoszenia i oceny                                                    |
|     6     | 5. Komunikacja                 | średni| wątki pod zgłoszeniem + powiadomienia; „szybkość komunikacji" w ocenie|
|     7     | 7. Middleman Innowacji (AI)    | średni| wariacja na matchmakingu (ten sam silnik AI)                          |
|   bonus   | Asystent kreatora, generator wniosków | średni | tylko po domknięciu reszty                                  |

## Zasady podziału

- **Kontrakt API to kod w `:core`**: DTO oraz trasy jako **Ktor Resources** (`@Resource`, wspólne dla serwera
  i klienta Ktor w przeglądarce) – bez ręcznego OpenAPI. Zmiana kontraktu = PR w `:core` zatwierdzony przez obie strony. Web nie czeka na backend: pracuje na **mockowanym repozytorium** za interfejsem i przełącza się
  na prawdziwe REST, gdy endpoint jest gotowy.
- Auth od początku przez Keycloak (PKCE, klient `hubmi-app`), role `user` / `admin`.
- Dane tylko **seedowe** (żadnych prawdziwych danych osobowych).
- Każda faza kończy się **działającym demo** na `main`; nie zostawiamy niedokończonych modułów.

## Faza 0 – Fundament (część wykonana)

| Status | Zadanie                                                       |
|:------:|:--------------------------------------------------------------|
|   ✅   | Ktor + Koin + Keycloak (JWT, role), docker compose, CI, ktlint|
|   ✅   | Mongo + Arrow w compose i zależnościach                       |
|   ⬜   | `ErrorResponse(code, message)`, `DomainError`, `respondError` (jedno miejsce mapowania na HTTP) |
|   ⬜   | Szablon zasobu end-to-end (route → service → repository) i wzorcowy test |
|   ⬜   | Kontrakt API v1 w `:core`: DTO + value classes ID, wspólne `Page<T>`        |
|   ⬜   | Ktor Resources w `:core` (`@Resource` per zasób) – serwer i klient web używają tych samych tras |

## Backend

### B1. Dane i katalog (moduł 2) – start po Fazie 0
- Kolekcje: `innovations`, `challenges`, `materials` + indeksy tekstowe i JSON Schema (`plugins/Indexes.kt`, `Schemas.kt`).
- `GET /api/innovations` (+ filtry: obszar, grupa docelowa, `q`, paginacja), `GET /api/innovations/{id}`,
  analogicznie `challenges`, `materials`.
- Zapis/edycja/archiwizacja: `POST|PUT|DELETE` – rola `admin`.
- **Seed**: ~30–50 przykładowych innowacji, wyzwań i materiałów (po polsku, na bazie Mapy Wyzwań i Biblioteki
  Innowacji od ROPS, bez danych osobowych).
- Pola mediów (wideo, obraz) jako URL – bez uploadu plików na MVP.

### B2. Matchmaking (moduł 1, obligatoryjny)
1. **Baseline (dzień 1)**: `POST /api/matches` – opis problemu → wyszukiwanie tekstowe Mongo (`$text`, polski stemming
   przy braku → normalizacja własna) po innowacjach i wcześniejszych potrzebach; ranking po `textScore`.
2. **AI lokalnie (kolejny krok)**: żadnych zewnętrznych serwisów AI. Embeddingi (np. `bge-m3`) i LLM (np. Bielik)
   uruchamiamy lokalnie przez Ollamę; ranking hybrydowy (tekst + wektor), LLM tylko do ekstrakcji słów kluczowych
   i krótkiego uzasadnienia. Indeks wektorowy w pamięci aplikacji (kilkaset rekordów), za interfejsem
   `MatchingEngine` – docelowo wymienialny na Mongo Vector Search.
   **Budżet PoC (M1 Pro, 32 GB):** Ollama działa natywnie na hoście (Metal, bez Dockera), serwer łączy się przez
   `host.docker.internal:11434`. Tryby `MATCHING_MODE`: `keyword` → `hybrid` (tekst + embeddingi, domyślny, bez LLM,
   ~dziesiątki ms) → `hybrid+llm` (opcjonalne uzasadnienia małym modelem). Embeddingi innowacji liczone raz przy seedzie.
3. Odpowiedź: lista dopasowań z **wyjaśnieniem** („dlaczego to pasuje": dopasowane obszary/frazy), podobne zgłoszone
   przypadki, link do innowacji. Wynik zapisujemy jako `need` (zasila trendy).
4. **Testy trafności** na zestawie seedowym: znane problemy → oczekiwane innowacje (próg top-3).
5. Fallback gdy AI niedostępne → ranking tekstowy (demo nie może się wywrócić).

### B3. Pomysły i testy (moduły 3, 4)
- `ideas`: fiszka (istota, adresaci, etap realizacji) – `POST /api/ideas` (zalogowany), `GET /api/ideas/mine`;
  statusy (`DRAFT → SUBMITTED → IN_REVIEW → ACCEPTED/REJECTED`) jako `sealed`/enum.
- `tests`: `POST /api/innovations/{id}/test-requests`, `POST /api/innovations/{id}/feedback` (ocena 1–5 + komentarz).
- Moderacja pomysłów: `PATCH /api/admin/ideas/{id}/status` + komentarz do autora.

### B4. Panel admina i trendy (moduł 6 + trendy z modułu 2)
- Agregacje Mongo na `needs`: liczba zgłoszeń per obszar/gmina/czas → `GET /api/admin/trends`.
- Kolejka zgłoszeń do weryfikacji, szybka publikacja/ukrycie treści.
- Dashboard: nowe pomysły, nieobsłużone pytania, najczęstsze potrzeby.

### B5. Komunikacja i powiadomienia (moduł 5)
- `threads` / `messages` powiązane z pomysłem lub potrzebą; role: autor, admin, ekspert (rola `expert` w Keycloak).
- Powiadomienia o nowym pomyśle/zmianie statusu: najpierw **lista w API** (`GET /api/notifications`),
  Web Push i e-mail jako rozszerzenie. Webhook do „bazy grantowej" jako punkt integracji (stub).

### B6. Middleman Innowacji (moduł 7) i nabory
- `POST /api/innovations/{id}/adaptations`: instytucja opisuje kontekst → AI proponuje dostosowanie innowacji do
  formy usługi (ten sam `MatchingEngine`/klient LLM co w B2).
- `calls` (nabory) + generator wniosku: szablon pól zależny od naboru, `GET /api/calls/active`.

### B7. Jakość i wdrożenie (równolegle)
- Testy endpointów (sukces / 401 / 403 / walidacja), testy repozytoriów z Testcontainers.
- Rate limiting i walidacja rozmiarów wejścia na endpointach z AI; brak logowania danych osobowych i tokenów.
- Deployment demo (np. VPS/Fly.io + `docker compose`), publiczny URL dla jurorów, backup seeda.
- Szacunek kosztów utrzymania (do prezentacji): hosting, Mongo, Keycloak, koszty LLM per zapytanie.

## Web (Kotlin/JS – `web/`)

Zamiast aplikacji mobilnej robimy **stronę webową**. Wybór technologii i uzasadnienie: sekcja „Decyzja: frontend" niżej.
Dostępność (WCAG 2.1 AA) to kryterium za 20%, więc **wchodzi do definicji ukończenia każdego widoku**.

### W0. Fundament
- `web/`: React 19 + TypeScript (Vite, Mantine, React Router); kontrakt z `:core` (DTO, `@Resource`) przez klienta
  Kotlin/JS `:web-client` eksportowanego do TS (`hubmi-client`). **Stan: szkielet działa** (status, logowanie Keycloak, konto).
- Klient HTTP: `HubApi` z `:web-client` (Ktor client + `ktor-client-resources`); mock zamiast serwera przez MSW lub `HubApi` z podmienionym `baseUrl`.
- Logowanie przez Keycloak (OIDC + PKCE w przeglądarce), token tylko w pamięci aplikacji.
- Dev: `jsBrowserDevelopmentRun` z proxy `/api → :8080` (bez CORS); produkcja: statyczny bundle w kontenerze nginx.
- Szkielet dostępności: `lang="pl"`, landmarki (`header/nav/main/footer`), link „Przejdź do treści", widoczny focus,
  design tokens z kontrastem ≥ 4.5:1, skalowanie do 200% bez utraty treści, `prefers-reduced-motion`.
- Fonty i zasoby **self-hosted** (bez CDN – prywatność, spójnie z zakazem zewnętrznych usług).

### W1. Matchmaking (obligatoryjny)
- Strona „Opisz problem": `<textarea>` z etykietą i licznikiem znaków, przykładowe problemy do kliknięcia.
- Wynik w regionie `aria-live="polite"`: karty innowacji z uzasadnieniem, „podobne zgłoszenia", akcja „Zgłoś potrzebę".
- Stany: ładowanie, brak wyników (z sugestią przeformułowania), błąd sieci (komunikat + ponów) – zawsze tekstem.
- Dyktowanie głosem: Web Speech API w Chrome wysyła audio do zewnętrznej usługi, więc **domyślnie wyłączone** albo
  pominięte (zakaz zewnętrznych usług); rozważyć lokalne rozpoznawanie mowy dopiero po domknięciu reszty.

### W2. Zasobnik wiedzy
- Lista + wyszukiwarka + filtry (obszar, grupa docelowa) innowacji; szczegóły z wideo (z napisami/transkrypcją) i opisem.
- Sekcje: wyzwania Małopolski (lista gmin/obszarów zamiast mapy jako alternatywa tekstowa), materiały edukacyjne.
- Przełączniki: duża czcionka, wysoki kontrast, prosty język.

### W3. Kreator pomysłów
- Formularz fiszki krok po kroku (istota → adresaci → etap), poprawnie powiązane etykiety i komunikaty błędów
  (`aria-describedby`), zapis szkicu w `localStorage`.
- Moje pomysły i status; w czasie naboru: formularz wniosku generowany z szablonu z backendu.
- Bonus: asystent AI (czat) rozwijający pomysł.

### W4. Tester i komunikacja
- Zgłoszenie do testów, ocena innowacji (grupa radio zamiast samych gwiazdek + komentarz), historia.
- Wątki z ROPS i ekspertem pod pomysłem, lista powiadomień (powiadomienia push i e-mail jako rozszerzenie).

### W5. Panel administratora (moduł 6)
- Ta sama aplikacja, trasa `/admin` dostępna dla roli `admin`: kolejka pomysłów, zmiana statusu i odpowiedź autorowi,
  edycja treści zasobnika, **trendy** potrzeb (wykresy z tekstową tabelą jako alternatywą).

### W6. UX/UI i materiały
- **Makiety UX/UI** (Figma) – wymagane minimum; wspólny design system z W0.
- Audyt dostępności: axe-core / Lighthouse w CI, ręcznie VoiceOver i klawiatura, przy 200% zoom i na telefonie.
- Test użyteczności na 3–5 osobach różnego wieku; wnioski do prezentacji.

## Decyzja: frontend webowy

| Opcja | Plusy | Minusy |
|:--|:--|:--|
| **React + TSX z klientem Kotlin/JS** – wybrana (zweryfikowana spike'iem, działa w repo) | prawdziwy DOM = natywna semantyka HTML; pełny ekosystem React/TS (Mantine, ESLint a11y); kontrakt z `:core` bez dublowania tras | warstwa mapująca DTO na typy przyjazne JS, wolniejsza pętla (Gradle → npm), ciężka paczka (~340 KB gzip razem z Mantine) |
| Kotlin/JS + `kotlin-wrappers` (React w Kotlinie) | wszystko w Kotlinie | mniejsza społeczność, UI po stronie mniej popularnych narzędzi |
| Compose Multiplatform Web (Wasm) | reużycie Compose (usuniętego już z repo modułu mobilnego) | rysuje na canvasie, a dostępność jest wciąż w rozwoju – ryzyko przy kryterium za 20%; duży bundle |
| Kobweb / Kilua (Compose HTML) | styl Compose, ale w DOM | projekty społeczności, mniej dokumentacji i pewności długoterminowej |

Jak to robimy stabilnie:
- Kontrakt (DTO, trasy `@Resource`) leży w `:core`; `:web-client` to cienka fasada wyeksportowana do TS, a UI jest zwykłym TSX.
- Na granicy Kotlin/JS ↔ TS tylko typy przyjazne JS, błędy jako `ApiResult` (bez wyjątków); mapowanie w jednym miejscu.
- Wersje Kotlina i Ktora w `libs.versions.toml`, zależności npm w `package-lock.json`, lockfile Yarna w `kotlin-js-store/`;
  ktlint, ESLint (`jsx-a11y`) i build w CI.
- Do rozważenia: gdyby paczka klienta okazała się za ciężka, zamienić go na cienki klient `fetch` w TS bez zmian w backendzie.

## Harmonogram (do dopasowania do czasu hackathonu)

| Faza | Backend                          | Web                                        | Wynik / demo                               |
|:----:|:---------------------------------|:-------------------------------------------|:-------------------------------------------|
|  0   | szkielet, kontrakt API (Resources) | W0 (szkielet, motyw, mock repo, auth)     | pusta aplikacja z logowaniem               |
|  1   | B1 (katalog + seed), B2 baseline | W2 (zasobnik), W1 na mocku                 | przeglądanie innowacji, wstępny matchmaking|
|  2   | B2 AI + testy trafności          | W1 na prawdziwym API                       | **minimalne kompletne demo (obligatoryjny moduł)** |
|  3   | B3, B4                           | W3, W4 (tester), W5                        | +3 moduły                                  |
|  4   | B5, B6                           | komunikacja, powiadomienia                 | +2 moduły, panel admina z trendami         |
|  5   | B7: deployment, koszty           | W6: audyt WCAG, polish                     | stabilne demo na publicznym URL            |
|  6   | zamrożenie kodu, bugfixy         | zamrożenie, nagranie demo                  | prezentacja PDF ≤10 slajdów / film ≤3 min  |

Reguła: po Fazie 2 mamy komplet na „obligatoryjny moduł + dobry UX"; wszystko dalej dokładamy tylko, jeśli poprzednie
fazy są domknięte.

## Dostarczane materiały (checklista)

- [ ] Nazwa i opis rozwiązania
- [ ] Prezentacja PDF (≤10 slajdów) **lub** film (≤3 min)
- [ ] Link do działającego dema + link do makiet UX/UI
- [ ] Szacunek kosztu utrzymania i zasobów
- [ ] Opis skalowalności i integracji (baza grantowa, powiadomienia) – kryterium „potencjał wdrożeniowy"
- [ ] Deklaracja dostępności (WCAG 2.1 AA) z wynikami audytu

## Ryzyka

| Ryzyko                                   | Zapobieganie                                                              |
|:-----------------------------------------|:--------------------------------------------------------------------------|
| AI niedostępne/wolne podczas demo        | fallback tekstowy, cache wyników dla scenariuszy demo, tryb offline       |
| Za mało realistycznych danych            | seed pisany wcześnie, scenariusze demo (3–4 historie) pisane razem z seedem|
| Rozjazd kontraktu BE ↔ web            | DTO i `@Resource` w `:core`, mock repo po stronie web                  |
| Dostępność „na końcu"                    | WCAG w definicji ukończenia widoku, audyt w fazie 5 tylko weryfikuje      |
| Wolny lokalny LLM (CPU)                  | mały model (Bielik 1.5B/4.5B), LLM opcjonalny, cache, wyniki bez LLM jako baza|
