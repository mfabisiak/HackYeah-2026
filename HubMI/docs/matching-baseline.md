# Baza trafności matchmakingu (silnik `keyword`)

Plik generuje test `MatchingQualityTest` (`WRITE_MATCHING_BASELINE=true ./gradlew :server:test`).
To punkt odniesienia dla silnika hybrydowego (BE-04, sekcja na końcu): hybryda daje wyższe hit@3 przy tym samym zestawie.

**Zestaw:** 48 zapytań z oczekiwanymi innowacjami
+ 11 negatywnych (bez dobrej odpowiedzi), na 36 innowacjach seedowych
(`server/src/test/resources/matching/golden.json`).

**Konfiguracja produkcyjna:** stemming „pierwsze 5 znaki”, BM25 `k1=1,2`, `b=0,75`,
wagi pól: tytuł ×3.0, słowa kluczowe ×3.0, streszczenie ×2.0, opis ×1.0, diagnoza problemu ×2.0, odbiorcy ×1.0, zmiana ×0.5, innowacyjność ×0.25, wizja ×0.25; próg `noGoodMatch` = 0,18; maks. 5 wyników.

## Wynik

| hit@3 | MRR | negatywy odrzucone |
|---|---|---|
| 83% | 0,79 | 82% |

Cele z BE-03: hit@3 ≥ 70%, ≥ 80% negatywów odrzuconych.

### Wg rejestru języka

| rejestr | zapytań | hit@3 | MRR |
|---|---|---|---|
| potoczny | 29 | 86% | 0,83 |
| urzędowy | 11 | 100% | 0,95 |
| senior | 6 | 33% | 0,33 |
| bez diakrytyków | 2 | 100% | 0,75 |

### Chybione zapytania (brak oczekiwanej innowacji w top 3)

- „Mój syn ma ataki paniki i boi się chodzić do szkoły” → oczekiwano: bezpieczna-przystan-mlodziezy
- „Dziadek upadł w domu i leżał kilka godzin zanim ktoś go znalazł” → oczekiwano: wirtualna-przychodnia-dla-seniora
- „Nie radzę sobie z telefonem, wnuczka mi tłumaczy, ale ja zapominam” → oczekiwano: cyfrowy-przewodnik-pokolen
- „Chciałabym iść na spacer, ale boję się wychodzić sama i potrzebuję kogoś do towarzystwa” → oczekiwano: sasiad-dla-seniora, rowerowy-wolontariat-dla-osob-starszych
- „Po operacji nóg potrzebuję ćwiczeń w domu, ale nie dojadę do przychodni” → oczekiwano: mobilny-punkt-rehabilitacji-wiejskiej
- „Po wypadku brat porusza się na wózku i nie może sam dostać się ani do pracy, ani do sklepu” → oczekiwano: transport-door-to-door-dla-niepelnosprawnych
- „Starsza pani z sąsiedztwa całymi dniami nie ma do kogo otworzyć ust” → oczekiwano: telefon-zaufania-dla-seniora-srebrna-linia, sasiad-dla-seniora
- „Chcemy z sąsiadami coś zorganizować na podwórku, ale nie mamy żadnych pieniędzy na start” → oczekiwano: platforma-mikrodotacji-dla-grup-nieformalnych

## Wpływ sekcji formularza aplikacyjnego ROPS

Innowacje niosą sekcje 4–8 formularza (innowacyjność, diagnoza problemu, odbiorcy, zmiana, wizja). Dla każdego
wariantu: najlepszy próg spośród tych, które odrzucają ≥ 90% negatywów.

| indeksowane pola | próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|---|
| z sekcjami formularza (produkcyjnie) | 0,26 | 0,58 | 0,56 | 91% |
| tylko tytuł, słowa kluczowe, streszczenie i opis | 0,16 | 0,54 | 0,51 | 91% |

Uwaga: sekcje seedu napisano na podstawie opisów innowacji, więc zysk jest przy tym zestawie zawyżony;
prawdziwe wnioski ROPS trzeba przemierzyć na własnych danych.

## Wybór normalizacji polskiego tekstu

Dla każdego wariantu: najlepszy próg spośród tych, które odrzucają ≥ 90% negatywów.

| wariant | próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|---|
| bez stemmingu | 0,16 | 0,44 | 0,44 | 91% |
| prefiks 4 znaków | 0,22 | 0,73 | 0,71 | 91% |
| prefiks 5 znaków | 0,26 | 0,58 | 0,56 | 91% |
| prefiks 6 znaków | 0,22 | 0,63 | 0,59 | 100% |
| prefiks 7 znaków | 0,21 | 0,58 | 0,58 | 91% |
| Lucene Stempel | 0,24 | 0,69 | 0,69 | 91% |

Najwyższy hit@3 ma: **prefiks 4 znaków**. Obcięcie do kilku znaków nie wymaga zależności i działa także na tekst
bez polskich znaków (diakrytyki są składane przy indeksowaniu i w zapytaniu).

## Kalibracja progu (wybrany wariant)

| próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|
| 0,05 | 0,96 | 0,90 | 18% |
| 0,06 | 0,96 | 0,90 | 18% |
| 0,07 | 0,96 | 0,90 | 27% |
| 0,08 | 0,96 | 0,90 | 27% |
| 0,09 | 0,96 | 0,90 | 36% |
| 0,10 | 0,96 | 0,90 | 45% |
| 0,11 | 0,94 | 0,88 | 45% |
| 0,12 | 0,92 | 0,85 | 55% |
| 0,13 | 0,92 | 0,85 | 64% |
| 0,14 | 0,92 | 0,85 | 73% |
| 0,15 | 0,88 | 0,83 | 73% |
| 0,16 | 0,85 | 0,81 | 73% |
| 0,17 | 0,83 | 0,79 | 82% |
| 0,18 | 0,83 | 0,79 | 82% |
| 0,19 | 0,81 | 0,77 | 82% |
| 0,20 | 0,75 | 0,72 | 82% |
| 0,21 | 0,75 | 0,72 | 82% |
| 0,22 | 0,73 | 0,70 | 82% |
| 0,23 | 0,73 | 0,70 | 82% |
| 0,24 | 0,71 | 0,68 | 82% |
| 0,25 | 0,63 | 0,60 | 82% |
| 0,26 | 0,58 | 0,56 | 91% |
| 0,27 | 0,56 | 0,54 | 91% |
| 0,28 | 0,54 | 0,53 | 91% |
| 0,29 | 0,54 | 0,53 | 91% |
| 0,30 | 0,52 | 0,51 | 100% |
| 0,31 | 0,48 | 0,47 | 100% |
| 0,32 | 0,46 | 0,45 | 100% |
| 0,33 | 0,44 | 0,43 | 100% |
| 0,34 | 0,42 | 0,41 | 100% |
| 0,35 | 0,40 | 0,39 | 100% |
| 0,36 | 0,38 | 0,36 | 100% |
| 0,37 | 0,35 | 0,34 | 100% |
| 0,38 | 0,31 | 0,30 | 100% |
| 0,39 | 0,29 | 0,28 | 100% |
| 0,40 | 0,27 | 0,26 | 100% |

Uwaga: zestaw jest mały i pisany przez zespół, więc próg może być przeuczony; przed demo dołożyć zapytania od
osób spoza zespołu. Próg 0,18 leży poniżej najwyższego wyniku negatywu (0,30): świadomie przepuszczamy najtrudniejszy negatyw, bo wyższy próg kosztuje hit@3.

## Silnik hybrydowy (BE-04)

BM25 (j.w.) + embeddingi `bge-m3` z lokalnej Ollamy. Wektory zestawu nagrano do
`server/src/test/resources/matching/embeddings.json.gz` (`WRITE_MATCHING_EMBEDDINGS=true`), więc pomiar
i test w CI nie wymagają modelu. Trafność: `(1 − w)·semantyka + w·tekst`, `w` = 0,40;
semantyka to cosinus przeskalowany z 0,35 (obcy tekst, 0) do
0,75 (niemal parafraza, 1); próg `noGoodMatch` = 0,25;
odcięcie względne 70% najlepszego wyniku; maks. 5 wyników.

| silnik | próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|---|
| keyword | 0,18 | 0,83 | 0,79 | 82% |
| hybrid | 0,25 | 1,00 | 0,95 | 91% |

### Wg rejestru języka

| rejestr | zapytań | hit@3 keyword | hit@3 hybrid | MRR keyword | MRR hybrid |
|---|---|---|---|---|---|
| potoczny | 29 | 86% | 100% | 0,83 | 0,96 |
| urzędowy | 11 | 100% | 100% | 0,95 | 1,00 |
| senior | 6 | 33% | 100% | 0,33 | 0,83 |
| bez diakrytyków | 2 | 100% | 100% | 0,75 | 1,00 |

### Chybione zapytania hybrydy

brak

### Kalibracja progu hybrydy

| próg | hit@3 | MRR | negatywy odrzucone |
|---|---|---|---|
| 0,15 | 1,00 | 0,95 | 64% |
| 0,16 | 1,00 | 0,95 | 64% |
| 0,17 | 1,00 | 0,95 | 64% |
| 0,18 | 1,00 | 0,95 | 64% |
| 0,19 | 1,00 | 0,95 | 64% |
| 0,20 | 1,00 | 0,95 | 64% |
| 0,21 | 1,00 | 0,95 | 73% |
| 0,22 | 1,00 | 0,95 | 73% |
| 0,23 | 1,00 | 0,95 | 82% |
| 0,24 | 1,00 | 0,95 | 91% |
| 0,25 | 1,00 | 0,95 | 91% |
| 0,26 | 1,00 | 0,95 | 91% |
| 0,27 | 0,98 | 0,93 | 91% |
| 0,28 | 0,98 | 0,93 | 100% |
| 0,29 | 0,98 | 0,93 | 100% |
| 0,30 | 0,98 | 0,93 | 100% |
| 0,31 | 0,96 | 0,92 | 100% |
| 0,32 | 0,92 | 0,90 | 100% |
| 0,33 | 0,90 | 0,89 | 100% |
| 0,34 | 0,88 | 0,86 | 100% |
| 0,35 | 0,88 | 0,86 | 100% |
| 0,36 | 0,85 | 0,85 | 100% |
| 0,37 | 0,85 | 0,85 | 100% |
| 0,38 | 0,83 | 0,83 | 100% |
| 0,39 | 0,81 | 0,81 | 100% |
| 0,40 | 0,77 | 0,77 | 100% |
| 0,41 | 0,71 | 0,71 | 100% |
| 0,42 | 0,71 | 0,71 | 100% |
| 0,43 | 0,69 | 0,69 | 100% |
| 0,44 | 0,69 | 0,69 | 100% |
| 0,45 | 0,65 | 0,65 | 100% |

Najniższy wynik trafnej innowacji w zestawie: 0,27; najwyższy wynik negatywu:
0,28. Próg leży poniżej najwyższego negatywu: świadomie przepuszczamy tematycznie sąsiednie zapytanie (brak w bibliotece, ale blisko „pomocy żywnościowej”), bo wyższy próg kosztuje trafne wyniki.
