# Demo dla jury (GitHub Pages)

Ta sama aplikacja React, ale zamiast serwera odpowiada **mock `HubApi`** napisany w Kotlinie (pakiet `web/mock/` w
`:web-client`). Nie potrzeba Mongo, Keycloaka ani Ollamy, więc całość da się wystawić jako statyczną stronę.

## Jak to działa

- `HubApi` w `:web-client` jest interfejsem z dwiema implementacjami: `createHttpHubApi(...)` (prawdziwy backend) i
  `createMockHubApi()` (demo). Wybór robi `web/src/api/hubApi.ts` po fladze `VITE_MOCK=true`.
- **Dane startowe** to dokładnie to, czym serwer zasila bazę (`server/src/main/resources/seed`: innowacje, wyzwania,
  materiały), plus kilka przykładowych pomysłów, naborów, wniosków, wątków i powiadomień dopisanych w `DemoSeed`.
  Pliki seedu trafiają do mocka przez zadanie Gradle `generateDemoSeed`, więc nie ma drugiej kopii danych.
- **Zapis**: każda zmiana (pomysł, ocena, zgłoszenie testów, wniosek, wiadomość, decyzja admina) ląduje w `localStorage`
  przeglądarki pod kluczem `hubmi.demo.db.v1`. Przeładowanie strony nic nie gubi, a każdy odwiedzający ma własne dane.
  Przycisk „Przywróć dane początkowe” w banerze demo czyści ten stan.
- **Użytkownik**: stały `jan.kowalski` z rolami `user` i `admin`, więc widać też panel admina. „Wyloguj” i „Zaloguj się”
  przełączają tylko stan w aplikacji.
- **„AI”** jest atrapą: asystent i Middleman składają odpowiedź z szablonów (podobne innowacje liczy prawdziwy, prosty
  silnik po rdzeniach słów) i strumieniują ją z opóźnieniem, jak prawdziwy model. Odpowiedzi są podpisane jako generowane.
- Mock nie sprawdza uprawnień i nie replikuje wszystkich walidacji serwera; to demo, nie zastępstwo backendu.

## Uruchomienie lokalnie

```bash
./gradlew :web-client:jsBrowserProductionLibraryDistribution   # klient Kotlin/JS (interfejs + obie implementacje)
cd web && npm install && npm run dev:demo                       # http://localhost:5173
```

## Publikacja na GitHub Pages

Workflow [.github/workflows/demo.yml](../../.github/workflows/demo.yml) buduje klienta Kotlin/JS i aplikację w trybie
demo, a po wejściu zmian na `main` publikuje ją na Pages. Na pull requestach tylko sprawdza, że demo się buduje.

Jednorazowo w repozytorium: **Settings → Pages → Build and deployment → Source: GitHub Actions**. Adres to
`https://<użytkownik>.github.io/<repozytorium>/`.

Ręczna budowa tego, co trafia na Pages (podstawowa ścieżka = nazwa repozytorium):

```bash
cd web && VITE_BASE=/HackYeah-2026/ npm run build:demo   # wynik w web/dist
```

- `VITE_BASE` ustawia ścieżkę bazową Vite i `basename` routera, bo strona stoi pod `/<repozytorium>/`.
- `build:demo` kopiuje `index.html` do `404.html`: Pages nie zna tras SPA, więc bezpośredni link, np. `/innowacje`,
  dostaje ten sam shell aplikacji, a React Router pokazuje właściwy widok.
