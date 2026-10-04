# Koszty utrzymania i zasoby (produkcja, self-hosting przez ROPS)

Dokument do BE-09 ([BACKEND_TICKETS.md](BACKEND_TICKETS.md)). Zakłada produkcyjne wdrożenie w infrastrukturze ROPS Kraków
(własna serwerownia albo wynajęte maszyny w UE), a nie demo. **Liczby ruchu to założenia, nie pomiary**; weryfikuje je test k6
z BE-09. Ceny z internetu są orientacyjne, bez VAT, z października 2026; ceny sprzętu i prądu do wyceny przez ROPS.

## Skala: Małopolska

- 3 429 342 mieszkańców (XII 2025), 22 powiaty, 182 gminy ([WUP Kraków](https://wupkrakow.praca.gov.pl/documents/d/wojewodzki-urzad-pracy-w-krakowie/informacja-miesieczna-wup-krakow-za-grudzien-2025)).
- Użytkownicy z loginem: pracownicy JST (gminne i powiatowe ośrodki pomocy społecznej, PCPR, CUS: rząd 500–1 000),
  organizacje pozarządowe (Kraków sam miał 4 614 organizacji w 2018 r., ale aktywnych w obszarze społecznym jest znacznie
  mniej; do ustalenia z ROPS), eksperci i admini ROPS.
- Anonimowi: mieszkańcy korzystający z matchmakingu (`POST /api/matches` działa bez logowania).

## Założenia o ruchu

| Scenariusz | Opis | Zalogowani | Wizyty / mies. | Dopasowania / mies. | Generacje AI / mies. |
|:--|:--|--:|--:|--:|--:|
| **Pilotaż** | rok 1, kilka gmin i organizacji partnerskich | 1 000 | 10 tys. | 1,5 tys. | 200 |
| **Region** | rok 2–3, wszystkie 182 gminy + NGO | 5 000 | 50 tys. | 8 tys. | 1 500 |
| **Kampania** | szczyt po akcji promocyjnej | 5 000 | 200 tys. | 30 tys. | 5 000 |

Przyjęte: wizyta ≈ 25 żądań API (statyka idzie z nginx), 12% dziennego ruchu w godzinie szczytu, skok kampanijny ×10 nad
szczyt. Wyjdzie z tego:

| Scenariusz | Szczyt (żądań/s) | Z ×10 | Dopasowań w godzinie szczytu |
|:--|--:|--:|--:|
| Pilotaż | ~0,3 | ~3 | ~6 |
| Region | ~1,4 | ~14 | ~30 |
| Kampania | ~5,6 | ~56 | ~120 |

**Wniosek: aplikacja nie jest wąskim gardłem.** Pojedyncza instancja Ktor/Netty obsłuży to z zapasem (do potwierdzenia
w k6). Dopasowanie to jeden embedding (dziesiątki ms) plus operacje w pamięci. Skalowanie dotyczy **AI** i **dostępności**,
nie przepustowości.

Dane: potrzeba to ok. 1–2 KB, więc 30 tys. miesięcznie to ~50 MB; wektory innowacji to 4 KB na rekord (500 innowacji = 2 MB,
w pamięci). Baza przez lata zmieści się w kilkunastu GB, kopia zapasowa także.

## Wąskie gardło: lokalny LLM

Pomiar z [ASSISTANT.md](ASSISTANT.md): Bielik 4,5B Q8 (5,1 GB) na M1 Pro daje ok. 25 tok/s, czyli plan adaptacji 15–18 s,
podpowiedzi 9–11 s. Generuje się **jedno żądanie naraz** (`LlmSlots(maxParallel = 1)`), a termin 25 s (asystent) i 40 s
(Middleman) liczy się razem z kolejką.

- **Obciążenie średnie jest znikome**: 1 500 generacji miesięcznie to ~50 dziennie, w godzinie szczytu ~10, czyli kilka %
  zajętości jednego modelu.
- **Ryzyko to seria jednoczesnych użytkowników**, np. szkolenie ROPS dla 30 osób. Przy jednym slocie i 25 s terminu
  obsłużone zostaną ok. 2 osoby, reszta dostanie `aiStatus = UNAVAILABLE` (nie błąd HTTP, ale też nie wynik). Na produkcji
  trzeba albo kilku slotów i `OLLAMA_NUM_PARALLEL` na GPU, albo kolejki z komunikatem „jesteś N-ty", albo wydłużonych terminów.
- Matchmaking nie zależy od LLM, a przy braku Ollamy działa na BM25 (fallback); awaria GPU nie wyłącza głównego modułu.
  To pozwala nie robić GPU w HA.

## Zasoby: warianty

Wymagania per komponent (szacunki, RAM w trybie ciągłym): Keycloak ~2 GB (min. 2 GB, [wg sizingu Skycloak](https://skycloak.io/blog/keycloak-production-sizing-ram-cpu-requirements/)),
serwer JVM 1–2 GB, Mongo 1–2 GB, Postgres (Keycloak) ~0,5 GB, Ollama z `bge-m3` ~1,5 GB, z Bielikiem Q8 +6 GB.

| Wariant | Topologia | Zasoby | Zastosowanie |
|:--|:--|:--|:--|
| **Minimum** | 1 VM aplikacji (compose) + 1 maszyna AI | VM: 4 vCPU, 16 GB RAM, 200 GB SSD; AI: GPU 16–24 GB VRAM, 32 GB RAM, 8 vCPU | pilotaż; brak HA, awaria = przestój do odtworzenia z kopii |
| **Zalecany (HA)** | 2× serwer + 2× Keycloak (LB z keepalived) + Mongo replica set 3 węzły + Postgres z repliką + 1 GPU | 2 vCPU/4 GB × 2 (serwer), 2 vCPU/4 GB × 2 (Keycloak), 2 vCPU/4 GB/100 GB × 3 (Mongo), Postgres 2 vCPU/4 GB | skala regionu, SLA ~99,5% |
| **Bez GPU** | j.w., Ollama na CPU, tylko `bge-m3` albo Bielik 1,5B | +2 vCPU, 4 GB RAM | najtaniej; LLM wyłączony (`ASSISTANT_ENABLED=false`) albo mały model (jakość niezmierzona) |

GPU: Bielik 4,5B Q8 i `bge-m3` mieszczą się w karcie 16 GB; 11B Q4_K_M potrzebuje ok. 7,4 GB VRAM na wagi, więc 24 GB
(np. L4) daje zapas na kilka równoległych slotów ([llmconfigurator](https://llmconfigurator.com/en/models/bielik/bielik-11b-v3)).
CPU bez GPU: modele 3B+ dają 5–10 tok/s (wg ogólnych benchmarków), więc plan Middlemana trwałby minutę i więcej;
Bielik 4,5B na CPU zmieściłby się w terminie tylko wyjątkowo.

Z mechanizmu repo: Mongo już jest replica setem, a indeks BM25 i wektory są **w pamięci każdej instancji** z TTL 5 min
(`InnovationIndex`). W wariancie HA zmiana innowacji jest widoczna na drugiej instancji do 5 minut; do natychmiastowej
spójności potrzebny jest sygnał unieważnienia (np. change streams). Świadoma decyzja, do opisania na slajdach.

## Koszty

### Jednorazowe

| Pozycja | Kwota | Uwagi |
|:--|:--|:--|
| Serwer z GPU (własny) | do wyceny; karta 16–24 GB używana ~2 tys. € ([L4 vs RTX 4000 Ada](https://getdeploying.com/gpus/nvidia-l4-vs-nvidia-rtx-4000-ada)), nowy serwer drożej | niepotrzebne, jeśli GPU wynajmujemy |
| Audyt bezpieczeństwa i test penetracyjny przed uruchomieniem | do wyceny | |
| Audyt dostępności WCAG 2.1 AA i deklaracja dostępności | do wyceny | ustawa o dostępności cyfrowej dla podmiotów publicznych |
| Wdrożenie, migracja treści, szkolenie redaktorów | nakład pracy | |

### Cykliczne (miesięcznie)

| Pozycja | Własna serwerownia | Wynajem w UE |
|:--|:--|:--|
| Maszyny aplikacji (Minimum) | koszt maszyn wirtualnych ROPS (marginalnie ~0, jeśli jest wolna wirtualizacja) | VPS 8–16 GB: ok. 10–40 € za sztukę ([Hetzner, ceny zmienne w 2026](https://www.vincentschmalbach.com/hetzner-cheap-cloud-unavailable-price-increases/)) |
| GPU | prąd ~150 W średnio ≈ 1,3 MWh/rok, chłodzenie, amortyzacja | serwer z RTX 4000 SFF Ada 20 GB: 184 € + 79 € jednorazowo ([GEX44](https://www.hetzner.com/de/dedicated-rootserver/gex44/)) |
| Kopie zapasowe (kilkanaście GB, szyfrowane, poza lokalizacją) | do wyceny | kilka € |
| Domena, certyfikaty (Let's Encrypt) | ~0 | ~0 |
| Monitoring (logi, metryki, alerty) | open source (Prometheus/Grafana lub Loki) | j.w. |
| **Razem sprzęt/hosting** | **zależny od istniejącej infrastruktury** | **Minimum ~240 €/mies. (~12 tys. zł/rok), bez GPU ~40–80 €/mies.** |

Sumy z wynajmu to rząd wielkości (2 maszyny VPS + GEX44 + kopie), nie oferta.

### Ludzie (zwykle największa pozycja)

| Rola | Szacowany nakład | Co obejmuje |
|:--|:--|:--|
| Administrator IT | 0,1–0,2 etatu | aktualizacje (Keycloak, Mongo, Postgres, Java), kopie i test odtwarzania, monitoring, certyfikaty |
| Redaktor / kurator treści | 0,25–0,5 etatu | biblioteka innowacji, materiały, aktualizacja Mapy Wyzwań, moderacja pomysłów, wątki z autorami, przegląd planów Middlemana |
| Rozwój i utrzymanie kodu | do ustalenia | poprawki, aktualizacje zależności, zmiany wzoru wniosku ROPS przy nowych naborach |

Kwot nie wyceniamy: stawki zależą od ROPS. Redaktor jest tym, co faktycznie decyduje o wartości platformy (aktualna
biblioteka = trafne dopasowania), więc nie należy go pomijać w budżecie.

## Wymagania organizacyjne przy self-hostingu

- **RODO**: konta, wątki i wnioski grantowe zawierają dane osobowe (e-mail, telefon, NIP/REGON, dane osób fizycznych).
  Potrzebne: rejestr czynności, IOD, retencja i procedura usunięcia konta, szyfrowane kopie. Dane i AI zostają u ROPS
  (Ollama lokalnie, brak zewnętrznych usług), co upraszcza podstawę prawną. Treści opisów potrzeb są oczyszczane z PESEL,
  telefonu i e-maila (`PersonalDataScrubber`) przed zapisem.
- **Dostępność cyfrowa**: deklaracja dostępności i audyt WCAG 2.1 AA wymagane od jednostek publicznych.
- **Tożsamość**: Keycloak może przyjąć konta z AD/LDAP ROPS lub federację; do ustalenia, czy ROPS tego chce.
- **Bezpieczeństwo operacyjne**: sieć wewnętrzna dla baz, sekrety poza repo, ograniczone konta admina, przegląd logów.

## Stan repo a wdrożenie produkcyjne (do zrobienia w BE-09)

Z przeglądu kodu i konfiguracji:

- Brak **rate limitingu**, limitu ciała żądania i obsługi `X-Forwarded-For`; za proxy każdy klient ma ten sam adres, więc
  limit po IP dla anonimowych dopasowań nie zadziała bez `XForwardedHeaders`.
- [`web/nginx.conf`](../web/nginx.conf) nie wyłącza buforowania (`proxy_buffering off`) dla `/api/`; strumienie SSE
  asystenta i Middlemana trzeba sprawdzić przez ten proxy.
- Keycloak: `start-dev`, `sslRequired: none`, redirecty na localhost, hasła `user/user`, `admin/admin`, `expert/expert`
  ([realm](../keycloak/hubmi-realm.json)); `KC_HOSTNAME` na sztywno. Wymaga trybu `start`, `KC_PROXY_HEADERS=xforwarded`,
  osobnego realmu produkcyjnego i adresu publicznego (`KEYCLOAK_ISSUER` musi się z nim zgadzać).
- Mongo bez uwierzytelniania i z wystawionym portem; produkcyjnie: auth + keyFile (replica set) i brak publikowanych portów.
- JVM bez `-Xmx` (domyślnie 25% pamięci kontenera); ustawić wprost.
- Brak metryk i audytu akcji admina; zostaje `/api/health/ready`.
- Seeder używa `$setOnInsert`, więc nie nadpisze treści; produkcyjnie `SEED=false` (dane demo to nie dane ROPS).
- `LlmSlots(maxParallel = 1)` i termin 25 s (patrz wyżej) do przemyślenia przed szkoleniami.
- Brak procedury deploya i aktualizacji (CI buduje obrazy tylko jako sprawdzian).

## Skalowanie na kolejne regiony

### Co dziś wiąże system z Małopolską

- **Nie ma pojęcia regionu (tenanta).** `region` innowacji i `municipality` potrzeby to wolne napisy, a korekta ×1,15
  w matchmakingu to `contains` na tekście (`matchesRegion`). Żadna kolekcja nie ma `regionId`, rola `admin` nie ma zakresu,
  jest jeden realm Keycloaka, a seed i treści są małopolskie.
- **Wzór wniosku jest ROPS-owy.** Moduł `calls` to Załącznik nr 3 ROPS Kraków (ok. 670 linii walidatora, `FORM_VERSION = 1`).
  Inne regiony mają własne wzory i zasady naborów.
- **Wektory innowacji nie są w Mongo.** Liczy je `VectorIndex` przy starcie i trzyma w pamięci każdej instancji (ok. 0,1 s
  na tekst na GPU, ok. 1 s na CPU wg [MATCHMAKING.md](MATCHMAKING.md)). Przy 36 innowacjach to 4–6 s, ale zimny start
  10 tys. innowacji to ok. 17 min na GPU i ok. 3 h na CPU, więc przy katalogu krajowym wektory trzeba utrwalić.

### Skala ogólnopolska (uproszczenie: ten sam profil użycia na mieszkańca, ×11 względem Małopolski)

| | Region (Małopolska) | Cała Polska |
|:--|--:|--:|
| Wizyty / mies. | 50 tys. | ~550 tys. |
| Szczyt (żądań/s), ze skokiem ×10 | ~1,4 / ~14 | ~15 / ~150 (kampania ~620) |
| Dopasowania / mies. | 8 tys. | ~90 tys. |
| Generacje AI / mies. | 1 500 | ~16 tys. (~550 dziennie, w godzinie szczytu ~110) |
| Przyrost bazy | kilkaset MB / rok | kilka GB / rok |

- **Aplikacja**: bezstanowa, więc skaluje się dokładaniem instancji (2–3 za LB wystarczą); sharding Mongo niepotrzebny,
  replica set wystarcza, bo dane rosną o rząd kilku GB rocznie.
- **AI**: ~16 tys. generacji to ok. 6–7 mln tokenów miesięcznie, czyli średnio ~7% jednego strumienia (35 tok/s), ale w
  godzinie szczytu ok. 40%. Przepustowościowo wystarczy 1 GPU z kilkoma slotami, drugi daje dostępność i obsługę szkoleń.
- **Keycloak**: ok. 50–60 tys. kont; klaster 2–3 węzłów z Postgresem z repliką.
- **Matching**: brute-force cosine na 10 tys. wektorów (1 024 wymiary) to ~10 mln operacji na zapytanie, więc ms; indeks
  w pamięci zajmuje dziesiątki MB. Limit to zimny start i spójność indeksów między instancjami (TTL 5 min), nie CPU.

Przy tej skali **ruch nie jest problemem; problemem są dane, wzór wniosku, tożsamość i operacje**.

### Trzy modele

| Model | Jak działa | Plusy | Minusy |
|:--|:--|:--|:--|
| **1. Instancja na region** | osobny stack (Mongo, Keycloak, serwer, web) z konfiguracją regionu; ten sam obraz | zero zmian modelu danych; każdy ROPS jest odrębnym administratorem danych; awaria nie rozlewa się | koszt i administracja ×N (patrz niżej); brak wymiany innowacji między regionami, a to sedno „Hubu"; N wersji do aktualizacji |
| **2. Jedna platforma wielodzierżawna** | `tenantId` w każdym agregacie, filtrze i indeksie; role admina per region; konfiguracja per tenant | jedna wersja, jeden zespół operacyjny, współdzielone GPU | duża refaktoryzacja; ryzyko wycieku między regionami (każdy filtr musi pilnować tenanta); wspólna odpowiedzialność za dane; jedno SLA dla wszystkich |
| **3. Hybryda** ⭐ | **wspólny krajowy katalog** innowacji, wyzwań i materiałów (publiczne, bez danych osobowych) + **regionalne** dane osobowe i operacyjne (potrzeby, pomysły, wnioski, wątki, konta) | innowacje krążą między regionami, a dane osobowe zostają u regionalnego ROPS; wspólne AI i silnik dopasowania | najwięcej pracy architektonicznej; trzeba uzgodnić, kto kuratoruje katalog |

**Rekomendacja, etapowo:**
1. Dziś: jedna instancja (Małopolska).
2. 2–3 regiony pilotażowo: **model 1** (ten sam obraz, konfiguracja per region). Szybkie i tanie w zmianach.
3. Przy 4+ regionach albo gdy regiony chcą wymieniać innowacje: wydzielić katalog krajowy (**model 3**).
Pełne multi-tenant (model 2) ma sens tylko wtedy, gdy jedna instytucja będzie operatorem całości.

### Zmiany w kodzie, niezależnie od modelu

1. **Kod TERYT zamiast napisu** w `region` i `municipality` (value class zgodnie z CLAUDE.md). Hierarchia gmina ⊂ powiat ⊂
   województwo zastępuje `contains`: dopasowanie „ten sam powiat" dostaje mniejszą korektę niż „ta sama gmina". Lista jednostek
   z TERYT jest publiczna (GUS).
2. **Wzór wniosku jako konfiguracja**: walidator `calls` za interfejsem z implementacją per wzór (`FORM_VERSION` już jest);
   ogólne walidatory (NIP, REGON, KRS, kody pocztowe, telefony) zostają wspólne.
3. **Trwałe wektory** (zapis z hashem `embeddingText` albo Mongo Vector Search) i **unieważnianie indeksu przez change streams**
   zamiast TTL 5 min, gdy katalog przekroczy 1–2 tys. pozycji albo działa więcej instancji.
4. **Keycloak**: realm na region albo jeden realm z organizacjami, role admina z zakresem regionu; federacja z IdP regionu.
5. **`SocialArea`**: wspólny zbiór krajowy w `:core` (rozszerzenie enumu to wydanie), nie lista wolna; regionalne
   niuanse jako słowa kluczowe.
6. Rate limiting i metryki z etykietą regionu, `LlmSlots` zasilane wspólnym klastrem Ollamy.

### Koszt: silosy a hybryda (rząd wielkości, 16 województw)

| | Model 1 (16 instancji) | Model 3 (hybryda) |
|:--|:--|:--|
| Sprzęt/hosting (wynajem w UE) | 16 × Minimum ≈ 16 × 240 € ≈ 3,8 tys. €/mies. (z własnym GPU w każdym regionie) | wspólny klaster HA + 2 GPU ≈ 0,6–1,2 tys. €/mies. + lekka regionalna baza i Keycloak per ROPS |
| Administracja IT | ~16 × 0,1–0,2 etatu | 1–2 etaty centralnie |
| Redaktor treści | 16 × 0,25–0,5 etatu | część katalogu centralnie (jedna kuracja), regiony tylko lokalne treści |

Ceny to ekstrapolacja z tabel wyżej, nie oferta. Argument za konsolidacją to przede wszystkim ludzie i jeden zakres kuracji
treści, nie serwery.

### Organizacyjnie

Każdy ROPS to osobny podmiot publiczny i administrator danych. Wspólny operator wymaga porozumienia i umów powierzenia
przetwarzania (art. 28 RODO), a model 3 dodatkowo ustalenia, kto finansuje i kuratoruje krajowy katalog.

## Do ustalenia z ROPS

1. Gdzie ma stać system: własna serwerownia, Urząd Marszałkowski czy wynajem w UE.
2. Czy ROPS dysponuje GPU albo budżetem na serwer z GPU, czy ma być wariant bez LLM.
3. Docelowy SLA i dopuszczalny przestój (decyduje o wyborze Minimum vs HA).
4. Kto jest administratorem IT i kto redaktorem treści (etaty z tabeli powyżej).
5. Prawdziwa liczba aktywnych organizacji i pracowników JST (popraw założenia ruchu).
6. Czy platforma ma obejmować kolejne regiony i kto byłby operatorem (decyduje o modelu 1, 2 lub 3).
