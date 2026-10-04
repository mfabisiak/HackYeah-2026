# Dane seedowe

Pliki JSON ładuje `DatabaseSeeder` (flaga `SEED=true`). Upsert po `_id` wyliczonym ze `slug` + `$setOnInsert`:
ponowny seed nie nadpisuje zmian ani archiwizacji zrobionych przez admina.

| Plik | Zawartość |
|---|---|
| `innovations.json` | 36 wymyślonych innowacji demonstracyjnych – baza testów trafności (`matching/golden.json`), **nie ruszać bez aktualizacji testów** |
| `rops-innovations.json` | 115 innowacji z [Biblioteki innowacji społecznych ROPS Kraków](https://rops.krakow.pl/innowacje-spoleczne/biblioteka-innowacji-spolecznych/kategorie) |
| `challenges.json`, `materials.json`, `samples.json` | wyzwania, materiały, przykład |

## Format `rops-innovations.json`

Tablica obiektów w formacie `SeedInnovationItem` (jak `innovations.json`), więc seeder nie potrzebuje osobnego kodu:

```json
{
  "slug": "rops-bawita",
  "title": "BaWita",
  "summary": "BaWita - tablica manipulacyjno terapeutyczna dla seniorów i osób z chorobami dementywnymi",
  "description": "…",
  "areas": ["AGING", "MENTAL_HEALTH"],
  "targetGroups": ["SENIORS"],
  "stage": "TESTED",
  "region": "Małopolska",
  "keywords": ["seniorzy", "osoby starsze"],
  "mediaUrls": ["<strona w bibliotece ROPS>", "<broszura PDF>", "<film>", "<paczka materiałów ZIP>"],
  "problemDiagnosis": "…",
  "audienceDescription": "…",
  "expectedChange": "…"
}
```

Odwzorowanie z karty innowacji na stronie ROPS:

| Sekcja na stronie ROPS | Pole |
|---|---|
| tytuł strony | `title` (`slug` = `rops-` + końcówka adresu) |
| krótki opis z listy kategorii | `summary` (≤ 280 znaków) |
| 1. Na czym polega rozwiązanie? | `description` |
| 2. Jakich problemów dotyczy innowacja? | `problemDiagnosis` |
| 3. Grupa docelowa + 4. Kto może skorzystać? | `audienceDescription` |
| 5. Czy to działa? | `expectedChange` |
| 6. Autorzy | **pomijane** (dane osobowe – wymóg wyzwania) |
| ikony: broszura, film, materiały | `mediaUrls` (pierwszy element to zawsze strona źródłowa) |

Pola, których ROPS nie publikuje (`innovativeness`, `futureVision`), zostają puste – niczego nie dopisujemy.

Pola **wyliczane** (heurystyka w skrypcie, do ręcznej korekty w pliku): `areas`, `targetGroups` (kategoria ROPS + słowa
kluczowe w tekście), `keywords` (nazwy kategorii), `stage` (`TESTED` – biblioteka zawiera innowacje przetestowane
i wybrane do upowszechniania), `region` (`Małopolska`).

## Odświeżenie danych

```bash
python3 scripts/scrape_rops.py            # z katalogu HubMI; tylko biblioteka standardowa Pythona
```

Skrypt jest deterministyczny i uprzejmy dla serwera (przerwa między żądaniami; `--cache-dir` pozwala nie pobierać
ponownie). Nowa kategoria na stronie ROPS przerywa działanie, dopóki nie zostanie dopisana do `CATEGORIES`.
Po odświeżeniu przejrzyj `git diff` – ręczne korekty w pliku zostaną nadpisane.

## Licencja i źródło

Treści pochodzą z publicznej strony ROPS Kraków; część innowacji jest udostępniona na licencji CC BY 4.0, reszta na
[zasadach wykorzystania innowacji MIIS](https://rops.krakow.pl/mpliki/IS/BIBLIOTEKA_INNOWACJI_SPOECZNYCH/Zasady_wykorzystania_innowacji_MIIS.pdf).
Każdy rekord linkuje do strony źródłowej w `mediaUrls`.
