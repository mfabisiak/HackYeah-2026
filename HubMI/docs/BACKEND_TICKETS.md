# Backlog backendu

Powiązane: [ROADMAP.md](ROADMAP.md) · [API.md](API.md) · [MATCHMAKING.md](MATCHMAKING.md) · [CLAUDE.md](../CLAUDE.md).
Rozmiary: **S** ≤ 2 h, **M** ≤ pół dnia, **L** ≈ dzień. Każdy ticket kończy się zielonym `./gradlew backendCheck`
i, jeśli zmienia kontrakt, aktualizacją [API.md](API.md). Zrobione: szkielet serwera, auth, Mongo, kontrakt z atrapami.

> Szczegółowe opisy (kontekst, stan repo, propozycje do zwalidowania, kryteria akceptacji, ryzyka) są w issues
> [#1–#10](https://github.com/mfabisiak/HackYeah-2026/issues). Ten plik to skrót; źródłem prawdy o zakresie są issues.

| ID    | Tytuł                                                   | Rozm. | Moduł | Zależy od |
|:------|:--------------------------------------------------------|:-----:|:-----:|:----------|
| BE-01 | Fundament: błędy, repozytoria, seed, testy z Mongo      |   M   |   –   | –         |
| BE-02 | Innowacje: odczyt, CRUD admina i dane seedowe           |   L   |   2   | BE-01     |
| BE-03 | Matchmaking v1: indeks tekstowy i `POST /api/matches`   |   L   |   1   | BE-02     |
| BE-04 | Matchmaking v2: lokalne AI (Ollama) i silnik hybrydowy  |   L   |   1   | BE-03     |
| BE-05 | Pomysły i nabory (fiszki, moderacja, wnioski)           |   M   |   3   | BE-01     |
| BE-06 | Tester: zgłoszenia do testów i oceny                    |   S   |   4   | BE-02     |
| BE-07 | Komunikacja: wątki i powiadomienia                      |   M   |   5   | BE-05     |
| BE-08 | Panel admina: trendy i liczniki                         |   M   |   6   | BE-03     |
| BE-09 | Demo: wdrożenie, koszty i test obciążeniowy             |   M   |   –   | BE-04     |
| BE-10 | Bonus: asystent kreatora i Middleman Innowacji (AI)     |   L   |  3, 7 | BE-04, 05 |

## BE-01 Fundament
- `DomainError` i jedno miejsce mapowania na HTTP (`respondError`), mapowanie `RepositoryError → DomainError`.
- Helpery repozytoriów Mongo (`Either.catch`, naruszenie unikalności → `Conflict`), indeksy przy starcie
  (idempotentnie), seeder uruchamiany flagą `SEED`.
- Paginacja (`PageRequest` z walidacją), kontekst zalogowanego użytkownika oraz rola `expert` w realmie.
- Testy repozytoriów z prawdziwym Mongo (Testcontainers).
- **AC:** wzorcowy zasób przechodzi route → service → repository z testem sukcesu, 401, 403 i walidacji.

## BE-02 Innowacje (moduł 2)
- Model, repozytorium i serwis; `GET /api/innovations` (filtry, paginacja) i `/{id}`; `POST`/`PUT`/`DELETE` dla admina.
- Wyzwania i materiały analogicznie (odczyt publiczny, zapis admin).
- Seed: 30–50 innowacji, ~10 wyzwań, ~15 materiałów po polsku, **bez danych osobowych**, pokrywające wszystkie `SocialArea`.
- **AC:** zastąpione atrapy z [API.md](API.md); recenzja merytoryczna seedu przez zespół.

## BE-03 Matchmaking v1 (moduł 1, obligatoryjny)
- Zestaw 30–50 złotych przypadków i harness mierzący hit@3 oraz MRR (baseline do porównań).
- Normalizacja polskiego tekstu, indeks BM25 w pamięci, oczyszczanie PESEL/telefonu/e-maila.
- `MatchingEngine` + `KeywordMatchingEngine`, `POST /api/matches` (zapis `need`, `noGoodMatch` poniżej progu,
  uzasadnienie z dopasowanych pól), feedback i „podobne zgłoszenia".
- **AC:** działa anonimowo, bez żadnych modeli; metryki zapisane jako baseline.

## BE-04 Matchmaking v2: lokalne AI
- Klienci Ollamy (embeddingi `bge-m3`, opcjonalnie LLM), embeddingi innowacji liczone przy zapisie, indeks wektorowy w pamięci.
- `HybridMatchingEngine` (RRF + korekty), `FallbackEngine` przy niedostępnej Ollamie, `MATCHING_MODE`,
  opcjonalne uzasadnienia przez LLM z walidacją odpowiedzi.
- **AC:** hit@3 wyższy niż baseline z BE-03; wyłączenie Ollamy nie psuje demo.

## BE-05 Pomysły i nabory (moduł 3)
- Fiszki (`POST`, `mine`, `{id}` dla autora lub admina), kolejka i zmiana statusu przez admina (niedozwolone przejścia → `409`).
- Nabory (`GET`, `active`, CRUD admina) z blokadą edycji dat/usuwania przy istniejących wnioskach.
- Generator i walidator wniosków grantowych wg oficjalnego wzoru ROPS (Załącznik nr 3):
  - 3 typy wnioskodawców (osoba fizyczna, podmiot, grupa nieformalna 1..5 partnerów).
  - Walidacja sum kontrolnych NIP, REGON, KRS, kodów pocztowych, telefonów, e-maili (value classes w domenie).
  - Walidacja harmonogramu i budżetu: max 3 mies. przygotowania, max 9 mies. testowania, kolejność etapów, limity kosztów bez ryzyka overflow (`toLong()`).
  - Idempotentny zapis wersji roboczej (`PUT`), optymistyczne blokowanie na `updatedAt`, atomowy limit max 2 złożonych wniosków na nabór na wnioskodawcę.
  - Oświadczenia i klauzule RODO z wersjonowaniem formularza i weryfikacją kompletności.
  - Zabezpieczenie uprawnień: tylko autor może modyfikować i składać swój wniosek (admin nie może edytować ani złożyć za autora).
- **AC:** testy własności zasobu, maszyny stanów, walidacji ROPS i uprawnień.

## BE-06 Tester (moduł 4)
- `test-requests` i `feedback` (ocena 1–5, jedna na użytkownika), agregacja `averageRating` w innowacji.

## BE-07 Komunikacja (moduł 5)
- Wątki i wiadomości dostępne tylko dla uczestników; powiadomienia z zdarzeń (nowy pomysł, zmiana statusu, wiadomość).
- Opcjonalnie: webhook wychodzący i e-mail (MailHog) jako punkt integracji z bazą grantową.

## BE-08 Panel admina (moduł 6)
- `GET /api/admin/trends` (agregacje `needs` po obszarze i gminie, zmiana względem poprzedniego okresu,
  liczba zapytań bez dopasowania) oraz liczniki dla dashboardu.

## BE-09 Demo
- Wdrożenie `docker compose` na publicznym URL z TLS, szacunek kosztów (hosting, RAM/GPU dla Ollamy, backup),
  test obciążeniowy (k6) dla listy innowacji i `POST /api/matches`, rate limiting, skrypt resetu danych demo.

## BE-10 Bonus
- `POST /api/ideas/{id}/assist` i `POST /api/innovations/{id}/adaptations` na lokalnym LLM.
- Stan i projekt: [ASSISTANT.md](ASSISTANT.md) (asystent z SSE i Middleman zaimplementowane; zostały cache, rate limiting,
  tryb `QUESTIONS` i ocena jakości).

## Kolejność
BE-01 → BE-02 → BE-03 → BE-04 (komplet obligatoryjnego modułu), potem moduły dodatkowe od najtańszych:
BE-05, BE-06, BE-08, BE-07, na koniec BE-09; BE-10 tylko, gdy reszta jest domknięta.
