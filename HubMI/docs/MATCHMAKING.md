# Matchmaking społeczny – projekt

Moduł obligatoryjny ([TASK.md](TASK.md), moduł 1). Ocena: *trafność dopasowania*, *łatwość zgłoszenia problemu*.
Całość działa **lokalnie** (żadnych zewnętrznych serwisów AI) i ma się uruchomić na laptopie deweloperskim
(Apple M1 Pro, 32 GB) – zob. „Budżet PoC".

## Co robi

Użytkownik opisuje problem własnymi słowami („mama mieszka sama na wsi i nie ma jak dojechać do lekarza").
System zwraca **3–5 innowacji z biblioteki ROPS** z uzasadnieniem, podobne zgłoszenia innych osób i – gdy nic
nie pasuje – uczciwe „brak dobrego dopasowania" z przejściem do zgłoszenia pomysłu.

## Przepływ

```
opis problemu
  → 1. oczyszczenie (normalizacja, wycięcie PESEL/telefonu/e-maila)
  → 2. [opcjonalnie LLM] ekstrakcja: obszar, grupa docelowa, słowa kluczowe, synonimy
  → 3. wyszukiwanie hybrydowe: tekst (BM25) + wektory (embeddingi)  → fuzja RRF
  → 4. korekty: obszar, grupa, region, dojrzałość innowacji, oceny z modułu Tester
  → 5. próg „brak dopasowania"
  → 6. uzasadnienie: z pól innowacji; [opcjonalnie LLM] skrócone i wygładzone
  → 7. zapis zapytania jako `need` (zasila trendy admina i zestaw testowy)
```

## Tryby (`MATCHING_MODE`)

| Tryb         | Składniki                                  | Wymaga              | Czas (szacunek) |
|:-------------|:-------------------------------------------|:--------------------|:----------------|
| `keyword`    | indeks tekstowy w aplikacji                | nic                 | ms              |
| `hybrid` ⭐  | tekst + embeddingi (`bge-m3`)              | Ollama              | dziesiątki ms   |
| `hybrid+llm` | j.w. + ekstrakcja i uzasadnienia (Bielik)  | Ollama + model LLM  | sekundy         |

Każdy tryb spada do niższego, gdy komponent jest niedostępny (timeout, brak Ollamy) – demo nie może się wywrócić.
Wyniki trybów `keyword` i `hybrid` są deterministyczne, więc nadają się do testów.

## Architektura (`server`)

```
matching/
  MatchingEngine        interface: suspend match(request): Either<MatchError, MatchResult>
  KeywordMatchingEngine BM25 po znormalizowanych polach (polski: własna normalizacja/lematyzacja słownikowa lub stemmer)
  HybridMatchingEngine  deleguje do EmbeddingClient + VectorIndex, fuzja RRF, korekty
  FallbackEngine        próbuje silnika głównego, przy błędzie używa prostszego
ports: EmbeddingClient (Ollama), LlmClient (Ollama), VectorIndex (w pamięci; później Mongo Vector Search)
```

- Indeks w pamięci aplikacji: kilkaset dokumentów, ładowany przy starcie; embeddingi innowacji liczone **raz**
  (przy seedzie/zapisie) i trzymane w dokumencie Mongo.
- Mongo `$text` nie ma stemmera dla polskiego. Docelowo przy większej skali: `mongot`
  (Atlas Search lokalnie lub self-hosted) za tym samym interfejsem.
- LLM może wybierać wyłącznie spośród ID kandydatów; każda odpowiedź walidowana (`Either`), tekst użytkownika to dane,
  nie instrukcje.

## Model danych

- `innovations`: tytuł, opis, obszar(y), grupy docelowe, etap (pomysł / przetestowana / wdrożona), region, media,
  `embedding` (lista liczb), oceny.
- `needs`: oczyszczony opis, wyekstrahowane obszar/region/słowa kluczowe, identyfikatory dopasowań,
  zgoda na użycie, informacja zwrotna („pomogło?"), znacznik czasu. Bez danych osobowych.

## API (Ktor Resources w `:core`)

- `POST /api/matches` – body `MatchRequest(description, municipality?)` → `MatchResult(matches[], similarNeeds[], noGoodMatch)`;
  `Match(innovationId, score, reasons[], matchedTerms[])`.
- `PUT /api/matches/{needId}/feedback` – „czy pomogło?" (tak/nie); idempotentne, zastępuje poprzednią odpowiedź.
- `GET /api/admin/trends` – agregacje `needs` (tylko admin).

## Budżet PoC (M1 Pro, 32 GB)

- Docker (limit ~7,75 GB) dla Mongo, Postgres, Keycloak, serwera ≈ 1,4 GB.
- **Ollama natywnie na hoście** (Metal), nie w Dockerze; serwer łączy się przez `host.docker.internal:11434`.
- Embeddingi: `bge-m3`. LLM: Bielik 1.5B/4.5B w kwantyzacji (11B tylko jeśli wystarczy czasu). Dostępność modeli
  w bibliotece Ollamy zweryfikować przy konfiguracji; awaryjnie wagi GGUF z Hugging Face.
- Zapytanie w trybie `hybrid` = 1 embedding (~dziesiątki ms) + operacje w pamięci.

## Prywatność i bezpieczeństwo

- Brak wysyłania danych poza maszynę; treści zapytań nie trafiają do logów.
- Wycinanie PESEL/telefonu/e-maila przed zapisem i przed przekazaniem do modeli.
- Rate limiting na `POST /api/matches`; limit długości opisu.
- Dane seedowe, bez prawdziwych danych osobowych (wymóg ROPS).

## Ocena trafności

Zestaw 30–50 realistycznych opisów problemów (po polsku, różne rejestry: potoczny, urzędowy, senior) z oczekiwanymi
innowacjami. Metryki: **hit@3** i **MRR** dla każdego trybu; test w CI dla `keyword`/`hybrid` (deterministyczne),
`hybrid+llm` na nagranych odpowiedziach. Wynik trafia do prezentacji jako dowód skuteczności.

## Pomysły wyróżniające

- **Luki jako dane**: zapytania bez dopasowania pokazują adminowi braki w bibliotece innowacji.
- **Dopasowanie odwrotne**: dla innowacji – gminy z największym zapotrzebowaniem (dla JST).
- **Przejrzystość**: czytelne uzasadnienie „dlaczego to pasuje" dla każdego wyniku.

## Stan wdrożenia v1 (BE-03, tryb `keyword`)

- `service/matching/`: `PersonalDataScrubber` (PESEL z sumą kontrolną, telefon, e-mail), `TextAnalyzer` (małe litery,
  stop-słowa, obcięcie do 5 znaków, składanie diakrytyków), `Bm25Index` (pola ważone, zob. niżej), `InnovationIndex`
  (indeks w pamięci, budowany leniwie, unieważniany przy zmianie innowacji i odświeżany co 5 minut),
  `KeywordMatchingEngine`.
- **Pola innowacji zgodne z formularzem aplikacyjnym ROPS** (załącznik nr 3 do ogłoszenia, pkt 3–8): opis innowacji
  (`description`), innowacyjność, diagnoza problemu, opis odbiorców, zmiana, wizja przyszłości. Wagi w indeksie:
  tytuł ×3, słowa kluczowe ×3, streszczenie ×2, opis ×1, diagnoza problemu ×2, odbiorcy ×1, zmiana ×0,5,
  innowacyjność ×0,25, wizja ×0,25 – najwięcej waży diagnoza, bo pisana jest językiem problemu, a tak samo pisze
  zgłaszający. Sekcje są opcjonalne (starsze wpisy ich nie mają). Dane pomysłodawcy (osoba, podmiot, grupa
  nieformalna), plan i koszty, kwota grantu, zespół i oświadczenia należą do wniosku grantowego (BE-05), nie do
  biblioteki, i nie są tu przechowywane.
- `score` w odpowiedzi to trafność 0–1: wynik BM25 podzielony przez najlepszy możliwy wynik dla tego zapytania, więc
  zapytania z obcymi słowami mają niższą trafność. Próg `noGoodMatch` to 0,18; frontend powinien pokazywać etykiety
  jakościowe, a nie procenty.
- Zapytanie jest zapisywane jako `need` (oczyszczony tekst, gmina, obszar najlepszego dopasowania, identyfikatory
  dopasowań, `helpful`); oryginalny opis nie trafia do bazy ani logów. **Login jest opcjonalny**: anonimowe
  zgłoszenie działa jak dotąd, a zalogowany zgłaszający zostaje właścicielem (`ownerId` = `sub` z Keycloaka). Właściciel
  to warunek powiadomień o zmianach i zaproszeń do testów (BE-07); tylko on może ocenić „czy pomogło?”, a anonimowe
  zgłoszenie ocenia się samym `needId`. Wymusić logowanie można, usuwając `optional = true` w `MatchRoutes`.
- „Podobne zgłoszenia” to najnowsze zgłoszenia dopasowane do tej samej innowacji (fragment ≤ 120 znaków).
- Trafność i dobór normalizacji: [matching-baseline.md](matching-baseline.md) (generowany testem
  `MatchingQualityTest`, `WRITE_MATCHING_BASELINE=true`). Zestaw złotych przypadków:
  `server/src/test/resources/matching/golden.json`.

Do potwierdzenia z ROPS / zespołem: czy `need` ma mieć TTL (np. 12 miesięcy) i czy w „podobnych zgłoszeniach”
pokazywać fragment tekstu, czy tylko obszar i liczbę.

## Kolejność wdrożenia

1. Model `innovations` + seed (30–50 rekordów) + zestaw testowy trafności.
2. `KeywordMatchingEngine` + `POST /api/matches` + testy hit@3.
3. Ollama w README, `EmbeddingClient`, `HybridMatchingEngine`, porównanie metryk.
4. Zapis `needs`, feedback, trendy dla admina.
5. (opcjonalnie) `hybrid+llm`.
