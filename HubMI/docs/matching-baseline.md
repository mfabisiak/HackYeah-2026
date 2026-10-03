# Baza trafności matchmakingu (silnik `keyword`)

Plik generuje test `MatchingQualityTest` (`WRITE_MATCHING_BASELINE=true ./gradlew :server:test`).
To punkt odniesienia dla silnika hybrydowego (BE-04): hybryda musi dać wyższe hit@3 przy tym samym zestawie.

**Zestaw:** 42 zapytań z oczekiwanymi innowacjami
+ 10 negatywnych (bez dobrej odpowiedzi), na 36 innowacjach seedowych
(`server/src/test/resources/matching/golden.json`).

**Konfiguracja produkcyjna:** stemming „pierwsze 5 znaki”, BM25 `k1=1,2`, `b=0,75`,
wagi pól: tytuł ×3.0, słowa kluczowe ×3.0, streszczenie ×2.0, opis ×1.0, diagnoza problemu ×2.0, odbiorcy ×1.0, zmiana ×0.5, innowacyjność ×0.25, wizja ×0.25; próg `noGoodMatch` = 0,18; maks. 5 wyników.

## Wynik

| hit@3 | MRR | negatywy odrzucone |
|---|---|---|
| 88% | 0,84 | 90% |

Cele z BE-03: hit@3 ≥ 70%, ≥ 80% negatywów odrzuconych.

### Wg rejestru języka

| rejestr | zapytań | hit@3 | MRR |
|---|---|---|---|
| potoczny | 25 | 92% | 0,88 |
| urzędowy | 10 | 100% | 1,00 |
| senior | 5 | 40% | 0,40 |
| bez diakrytyków | 2 | 100% | 0,67 |

### Chybione zapytania (brak oczekiwanej innowacji w top 3)

- „Mój syn ma ataki paniki i boi się chodzić do szkoły” → oczekiwano: bezpieczna-przystan-mlodziezy
- „Dziadek upadł w domu i leżał kilka godzin zanim ktoś go znalazł” → oczekiwano: wirtualna-przychodnia-dla-seniora
- „Nie radzę sobie z telefonem, wnuczka mi tłumaczy, ale ja zapominam” → oczekiwano: cyfrowy-przewodnik-pokolen
- „Chciałabym iść na spacer, ale boję się wychodzić sama i potrzebuję kogoś do towarzystwa” → oczekiwano: sasiad-dla-seniora, rowerowy-wolontariat-dla-osob-starszych
- „Po operacji nóg potrzebuję ćwiczeń w domu, ale nie dojadę do przychodni” → oczekiwano: mobilny-punkt-rehabilitacji-wiejskiej

## Wpływ sekcji formularza aplikacyjnego ROPS

Innowacje niosą sekcje 4–8 formularza (innowacyjność, diagnoza problemu, odbiorcy, zmiana, wizja). Dla każdego
wariantu: najlepszy próg spośród tych, które odrzucają ≥ 90% negatywów.

| indeksowane pola | próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|---|
| z sekcjami formularza (produkcyjnie) | 0,17 | 0,88 | 0,84 | 90% |
| tylko tytuł, słowa kluczowe, streszczenie i opis | 0,16 | 0,64 | 0,60 | 90% |

Uwaga: sekcje seedu napisano na podstawie opisów innowacji, więc zysk jest przy tym zestawie zawyżony;
prawdziwe wnioski ROPS trzeba przemierzyć na własnych danych.

## Wybór normalizacji polskiego tekstu

Dla każdego wariantu: najlepszy próg spośród tych, które odrzucają ≥ 90% negatywów.

| wariant | próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|---|
| bez stemmingu | 0,14 | 0,52 | 0,51 | 90% |
| prefiks 4 znaków | 0,20 | 0,81 | 0,76 | 90% |
| prefiks 5 znaków | 0,17 | 0,88 | 0,84 | 90% |
| prefiks 6 znaków | 0,14 | 0,86 | 0,81 | 90% |
| prefiks 7 znaków | 0,14 | 0,79 | 0,77 | 90% |
| Lucene Stempel | 0,15 | 0,90 | 0,86 | 90% |

Najwyższy hit@3 ma: **Lucene Stempel**. Obcięcie do kilku znaków nie wymaga zależności i działa także na tekst
bez polskich znaków (diakrytyki są składane przy indeksowaniu i w zapytaniu).

## Kalibracja progu (wybrany wariant)

| próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|
| 0,05 | 0,98 | 0,91 | 20% |
| 0,06 | 0,98 | 0,91 | 20% |
| 0,07 | 0,98 | 0,91 | 30% |
| 0,08 | 0,98 | 0,91 | 30% |
| 0,09 | 0,98 | 0,91 | 40% |
| 0,10 | 0,95 | 0,90 | 50% |
| 0,11 | 0,95 | 0,90 | 50% |
| 0,12 | 0,93 | 0,88 | 60% |
| 0,13 | 0,93 | 0,88 | 70% |
| 0,14 | 0,93 | 0,88 | 80% |
| 0,15 | 0,90 | 0,87 | 80% |
| 0,16 | 0,90 | 0,87 | 80% |
| 0,17 | 0,88 | 0,84 | 90% |
| 0,18 | 0,88 | 0,84 | 90% |
| 0,19 | 0,86 | 0,82 | 90% |
| 0,20 | 0,83 | 0,79 | 90% |
| 0,21 | 0,83 | 0,79 | 90% |
| 0,22 | 0,79 | 0,75 | 90% |
| 0,23 | 0,79 | 0,75 | 90% |
| 0,24 | 0,76 | 0,72 | 90% |
| 0,25 | 0,67 | 0,64 | 90% |
| 0,26 | 0,62 | 0,59 | 90% |
| 0,27 | 0,60 | 0,57 | 90% |
| 0,28 | 0,57 | 0,56 | 90% |
| 0,29 | 0,57 | 0,56 | 90% |
| 0,30 | 0,55 | 0,53 | 90% |
| 0,31 | 0,52 | 0,51 | 100% |
| 0,32 | 0,48 | 0,46 | 100% |
| 0,33 | 0,43 | 0,43 | 100% |
| 0,34 | 0,43 | 0,43 | 100% |
| 0,35 | 0,40 | 0,40 | 100% |
| 0,36 | 0,36 | 0,36 | 100% |
| 0,37 | 0,33 | 0,33 | 100% |
| 0,38 | 0,31 | 0,31 | 100% |
| 0,39 | 0,26 | 0,26 | 100% |
| 0,40 | 0,26 | 0,26 | 100% |

Uwaga: zestaw jest mały i pisany przez zespół, więc próg może być przeuczony; przed demo dołożyć zapytania od
osób spoza zespołu. Próg 0,18 leży poniżej najwyższego wyniku negatywu (0,30): świadomie przepuszczamy najtrudniejszy negatyw, bo wyższy próg kosztuje hit@3.
